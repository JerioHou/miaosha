package com.jerio.miaosha.rabbitmq;

import com.jerio.miaosha.domain.MiaoshaOrder;
import com.jerio.miaosha.domain.MiaoshaUser;
import com.jerio.miaosha.redis.RedisService;
import com.jerio.miaosha.service.GoodsService;
import com.jerio.miaosha.service.MiaoshaService;
import com.jerio.miaosha.service.OrderService;
import com.jerio.miaosha.vo.GoodsVo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Jerio on 2018/9/1
 */
@Component
public class MQReciver {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;


    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void recive(String message){
      MiaoshaMessage mm =  RedisService.stringToBean(message, MiaoshaMessage.class);

        MiaoshaUser user = mm.getUser();
        long goodsId = mm.getGoodsId();

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if(stock <= 0) {
            return;
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if(order != null) {
            return;
        }
        //减库存 下订单 写入秒杀订单
        miaoshaService.miaosha(user, goods);
    }
}
