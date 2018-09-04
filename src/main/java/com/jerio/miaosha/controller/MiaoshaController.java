package com.jerio.miaosha.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.jerio.miaosha.annotation.AccessLimit;
import com.jerio.miaosha.domain.MiaoshaOrder;
import com.jerio.miaosha.domain.MiaoshaUser;
import com.jerio.miaosha.domain.OrderInfo;
import com.jerio.miaosha.rabbitmq.MQSender;
import com.jerio.miaosha.rabbitmq.MiaoshaMessage;
import com.jerio.miaosha.redis.GoodsKey;
import com.jerio.miaosha.redis.RedisService;
import com.jerio.miaosha.result.CodeMsg;
import com.jerio.miaosha.result.Result;
import com.jerio.miaosha.service.GoodsService;
import com.jerio.miaosha.service.MiaoshaService;
import com.jerio.miaosha.service.MiaoshaUserService;
import com.jerio.miaosha.service.OrderService;
import com.jerio.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jerio on 2018/3/20.
 */
@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {
    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    MQSender sender;

    private HashMap<Long, Integer> localOverMap =  new HashMap<Long, Integer>();
    private AtomicInteger times = new AtomicInteger(0);
    final RateLimiter limiter = RateLimiter.create(200.0);//每秒放入200个token

    /**
     * 系统初始化
     * */
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if(goodsList == null) {
            return;
        }
        for(GoodsVo goods : goodsList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getStockCount());
            localOverMap.put(goods.getId(), goods.getStockCount());
        }
    }

//    @RequestMapping("/do_miaosha")
//    public String list(Model model, MiaoshaUser user,
//                       @RequestParam("goodsId")long goodsId) {
//        model.addAttribute("user", user);
//        if(user == null) {
//            return "login";
//        }
//        //判断库存
//        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
//        int stock = goods.getStockCount();
//        if(stock <= 0) {
//            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
//            return "miaosha_fail";
//        }
//        //判断是否已经秒杀到了
//        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
//        if(order != null) {
//            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
//            return "miaosha_fail";
//        }
//        //减库存 下订单 写入秒杀订单
//        OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
//        model.addAttribute("orderInfo", orderInfo);
//        model.addAttribute("goods", goods);
//        return "order_detail";
//    }
//
    @RequestMapping(value="/{path}/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model,MiaoshaUser user,
                                   @PathVariable("path")String path,
                                   @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            System.out.println("用户不存在");
            return Result.error(CodeMsg.SESSION_ERROR);
        }

//        验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if(!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        //内存标记，减少redis访问
        times.incrementAndGet();
        if(times.get() > localOverMap.get(goodsId)) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if(order != null) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        //预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, ""+goodsId);//10
        if(stock < 0) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0);//排队中
    }

    @AccessLimit
    @RequestMapping(value="/verifyCode", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCod(HttpServletResponse response, MiaoshaUser user,
                                              @RequestParam("goodsId")long goodsId) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try {
            BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }

    @AccessLimit
    @RequestMapping(value="/path", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam(value="verifyCode", defaultValue="0")int verifyCode
    ) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        if(!limiter.tryAcquire()) { //未请求到limiter则立即返回false
            return Result.error(CodeMsg.TOO_MANY_REQUIRES);
        }

        //验证秒杀是否开始
        //未开始 或 已结束，都不暴露秒杀地址
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Date endTime = goods.getEndDate();
        Date startTime = goods.getStartDate();
        Date nowTime = new Date();
        if (endTime.getTime() < nowTime.getTime() || startTime.getTime() > nowTime.getTime()){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
        if(!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        String path  =miaoshaService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }

    /**
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     * */
    @AccessLimit
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }
}
