package com.jerio.miaosha.Access;

import com.jerio.miaosha.domain.MiaoshaUser;

/**
 * Created by Jerio on 2018/9/3
 */
public class UserHolder {

    public static ThreadLocal<MiaoshaUser> holder = new ThreadLocal<MiaoshaUser>();

    public static void setUser(MiaoshaUser miaoshaUser){
        holder.set(miaoshaUser);
    }

    public static MiaoshaUser getUser(){
        return holder.get();
    }
}
