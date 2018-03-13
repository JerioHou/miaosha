package com.jerio.miaosha.redis;

/**
 * Created by Jerio on 2018/3/13.
 */
public interface KeyPrefix {
    int expireSeconds();
    String getPrefix();
}
