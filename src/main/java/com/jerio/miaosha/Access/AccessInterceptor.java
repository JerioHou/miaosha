package com.jerio.miaosha.Access;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;
import com.jerio.miaosha.annotation.AccessLimit;
import com.jerio.miaosha.domain.MiaoshaUser;
import com.jerio.miaosha.result.CodeMsg;
import com.jerio.miaosha.result.Result;
import com.jerio.miaosha.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * Created by Jerio on 2018/9/3
 */
@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {
    final RateLimiter limiter = RateLimiter.create(200.0);//每秒放入200个token

    @Autowired
    private MiaoshaUserService userService;

    //拦截请求，根据token获取用户信息
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取用户信息
        MiaoshaUser miaoshaUser = getUser(request,response);
        if (miaoshaUser != null){
            UserHolder.setUser(miaoshaUser);
        }
        //对于带有 @AccessLimit注解 rateLimiter = true 的方法,接口限流
        if (handler instanceof HandlerMethod){
            HandlerMethod  hm = (HandlerMethod) handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if (accessLimit ==null){
                return true;
            }
            //校验登录状态
            if (accessLimit.needLogin()){
                if(miaoshaUser == null) {
                    //未请求到limiter则立即返回false
                    render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
            }
            //接口限流
            if (accessLimit.rateLimiter()){
                if(!limiter.tryAcquire()) {
                    //未请求到limiter则立即返回false
                    render(response, CodeMsg.TOO_MANY_REQUIRES);
                    return false;
                }
            }
        }
        return true;
    }


    private MiaoshaUser getUser(HttpServletRequest request, HttpServletResponse response){
        String paramToken = request.getParameter(MiaoshaUserService.COOKI_NAME_TOKEN);
        String cookieToken = getCookieValue(request,MiaoshaUserService.COOKI_NAME_TOKEN);
        if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        String token = StringUtils.isEmpty(cookieToken)? paramToken:cookieToken;
        MiaoshaUser miaoshaUser = userService.getByToken(response,token);
        return miaoshaUser;
    }


    private String getCookieValue(HttpServletRequest request, String cookiNameToken) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0){
            return null;
        }
        for (Cookie cookie : cookies){
            if (cookie.getName().equals(cookiNameToken)){
                return cookie.getValue();
            }
        }
        return null;
    }

    private void render(HttpServletResponse response, CodeMsg cm)throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str  = JSON.toJSONString(Result.error(cm));
        out.write(str.getBytes("UTF-8"));
        out.flush();
        out.close();
    }
}
