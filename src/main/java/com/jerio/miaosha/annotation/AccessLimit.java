package com.jerio.miaosha.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Jerio on 2018/9/3
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessLimit {

    boolean needLogin() default true;
}