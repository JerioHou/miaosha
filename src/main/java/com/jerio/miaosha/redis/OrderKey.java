package com.jerio.miaosha.redis;

/**
 * Created by Jerio on 2018/9/1
 */
public class OrderKey extends BasePrefix{
    public OrderKey(String prefix) {
        super(prefix);
    }

    public static OrderKey getMiaoshaOrderByUidGid = new OrderKey("moug");

}
