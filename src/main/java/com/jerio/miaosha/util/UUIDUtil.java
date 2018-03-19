package com.jerio.miaosha.util;

import java.util.UUID;

/**
 * Created by Jerio on 2018/3/19.
 */
public class UUIDUtil {
    public static String uuid(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
