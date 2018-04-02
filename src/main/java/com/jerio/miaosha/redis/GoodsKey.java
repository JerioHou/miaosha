package com.jerio.miaosha.redis;

/**
 * Created by Jerio on 2018/3/19.
 */
public class GoodsKey extends BasePrefix {
    public static final int GOODLIST_EXPIRE = 60;
    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static GoodsKey goodList = new GoodsKey(GOODLIST_EXPIRE, "gl");
    public static GoodsKey goodDetail = new GoodsKey(GOODLIST_EXPIRE, "gd");
}
