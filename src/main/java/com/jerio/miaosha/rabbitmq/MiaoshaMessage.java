package com.jerio.miaosha.rabbitmq;

import com.jerio.miaosha.domain.MiaoshaUser;

/**
 * Created by Jerio on 2018/9/1
 */
public class MiaoshaMessage {
    private MiaoshaUser user;
    private long goodsId;
    public MiaoshaUser getUser() {
        return user;
    }
    public void setUser(MiaoshaUser user) {
        this.user = user;
    }
    public long getGoodsId() {
        return goodsId;
    }
    public void setGoodsId(long goodsId) {
        this.goodsId = goodsId;
    }
}
