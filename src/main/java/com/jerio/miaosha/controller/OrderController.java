package com.jerio.miaosha.controller;

import com.jerio.miaosha.annotation.AccessLimit;
import com.jerio.miaosha.domain.MiaoshaUser;
import com.jerio.miaosha.domain.OrderInfo;
import com.jerio.miaosha.redis.RedisService;
import com.jerio.miaosha.result.CodeMsg;
import com.jerio.miaosha.result.Result;
import com.jerio.miaosha.service.GoodsService;
import com.jerio.miaosha.service.MiaoshaUserService;
import com.jerio.miaosha.service.OrderService;
import com.jerio.miaosha.vo.GoodsVo;
import com.jerio.miaosha.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by Jerio on 2018/9/2
 */
@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    @AccessLimit
    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> info(Model model, MiaoshaUser user,
                                      @RequestParam("orderId") long orderId) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        OrderInfo order = orderService.getOrderById(orderId);
        if(order == null) {
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }
        long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOrder(order);
        vo.setGoods(goods);
        return Result.success(vo);
    }
}
