package com.jerio.miaosha.Access;

import com.jerio.miaosha.annotation.AccessLimit;
import com.jerio.miaosha.domain.MiaoshaUser;
import com.jerio.miaosha.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Jerio on 2018/9/3
 */
@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private MiaoshaUserService userService;

    //拦截请求，根据token获取用户信息
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //对于带有 @AccessLimit注解 needLogin = true 的方法
        //获取用户信息
        if (handler instanceof HandlerMethod){
            HandlerMethod  hm = (HandlerMethod) handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if(accessLimit !=null && accessLimit.needLogin()){
                MiaoshaUser miaoshaUser = getUser(request,response);
                if (miaoshaUser == null){
                    return false;
                }
                UserHolder.setUser(miaoshaUser);
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
}
