package com.jerio.miaosha.init;

import com.google.common.util.concurrent.RateLimiter;
import com.jerio.miaosha.annotation.AccessLimit;
import com.jerio.miaosha.util.AnnotationUtil;
import com.jerio.miaosha.util.ClassUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Franky on 2018/09/13
 *
 * 扫描Controller包下的 所有类的 所有方法
 * 对于带有AccessLimit 且 rateLimiter == ture 的方法，
 * 生成对应的com.google.common.util.concurrent.RateLimiter 对象
 *
 */
@Component
public class InitRateLimiter implements CommandLineRunner {

    @Value("${rateLimiter.package}")
    private String packageName;
    //不允许修改，只提供get方法
    private static final HashMap<String,RateLimiter> rateLimiterMap = new HashMap<String,RateLimiter>();

    public static RateLimiter getRateLimiter(String rateLimiterName) {

        return rateLimiterMap.get(rateLimiterName);
    }

    @Override
    public void run(String... strings) throws Exception {
        loadRateLimiter();
    }

    private void loadRateLimiter() {
        // 获取包下所有类
        List<Class<?>> classes = ClassUtil.getClassesByPackageName(packageName);
        // 遍历类
        for (Class clazz : classes) {
            Method[] methods = clazz.getMethods();
            if (methods == null || methods.length == 0){
                return ;
            }

            //遍历函数
            for (Method method : methods) {
                AccessLimit accessLimit = AnnotationUtil.getAnnotationValueByMethod(method, AccessLimit.class);
                //开启限流 且 限流器名字不为空 且 限流速率 > 0
                if (accessLimit !=null && accessLimit.rateLimiter()
                        && !StringUtils.isEmpty(accessLimit.rateLimiterName())
                        && accessLimit.rateLimiterValue() > 0) {
                    String rateLimiterName = accessLimit.rateLimiterName();
                    double rateLimiterValue = accessLimit.rateLimiterValue();
                    if(rateLimiterMap.get(rateLimiterName) == null) {
                        RateLimiter rateLimiter = RateLimiter.create(rateLimiterValue);
                        rateLimiterMap.put(rateLimiterName,rateLimiter);
                    }
                }
            }
        }
    }
}
