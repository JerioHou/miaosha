package com.jerio.miaosha.controller;

import com.jerio.miaosha.annotation.AccessLimit;
import com.jerio.miaosha.domain.MiaoshaOrder;
import com.jerio.miaosha.domain.MiaoshaUser;
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

    private HashMap<Long, Boolean> localOverMap =  new HashMap<Long, Boolean>();

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
            localOverMap.put(goods.getId(), false);
        }
    }

    @AccessLimit
    @RequestMapping(value="/{path}/do_miaosha", method= RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model,MiaoshaUser user,
                                   @PathVariable("path")String path,
                                   @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);

        //验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if(!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        /*
        内存标记，部分减少redis访问
        hashmap是有线程安全问题的，但是在这里，localOverMap初始化后并不会增加新的key
        而是不停地对已有的key 覆盖其值。
        同时如果线程A get操作后被挂起，而线程B修改了localOverMap的值，那么本次修改对线程A不可见.
        因此会出现秒杀结束但有线程能继续执行下面的代码，特别是线程数量特别多的情况下，
        所以此处只是部分减少redis请求，但仍能启动一定作用，特别是没有 “预减库存”时，能较大程度较少消息的数量
        */
        boolean over = localOverMap.get(goodsId);
        if(over) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if(order != null) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        /*
        预减库存
        此处会存在少买的可能性，即redis减库存成功，但DB减库存失败
        预减库存的好处是能极大地较少 消息的数量，提高系统的响应速度。
        reids是单线程的，所以decr是线程安全的，所以不用担心超卖问题，同时数据库也做了超卖的限制

        如果对库存要求能严格，不能少也不能多，则不能采用这种方式。
        */
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, ""+goodsId);//10
        if(stock < 0) {
            localOverMap.put(goodsId,true);
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

    @AccessLimit(rateLimiter = true)
    @RequestMapping(value="/path", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam(value="verifyCode", defaultValue="0")int verifyCode
    ) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
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
    public Result<String> miaoshaResult(Model model,MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);

        long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        //long类型在前端会出现精度丢失问题，故采用string类型传输
        return Result.success(String.valueOf(result));
    }
}
