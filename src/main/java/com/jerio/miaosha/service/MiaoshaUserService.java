package com.jerio.miaosha.service;

import com.jerio.miaosha.dao.MiaoshaUserDao;
import com.jerio.miaosha.domain.MiaoshaUser;
import com.jerio.miaosha.exception.GlobalException;
import com.jerio.miaosha.result.CodeMsg;
import com.jerio.miaosha.util.MD5Util;
import com.jerio.miaosha.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by Jerio on 2018/3/14.
 */
@Service
public class MiaoshaUserService {

    @Autowired
    private MiaoshaUserDao miaoshaUserDao;

    public MiaoshaUser getById(long id) {
        return miaoshaUserDao.getById(id);
    }

    public boolean login(HttpServletResponse response, LoginVo loginVo) {
        if(loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        //判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if(user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //验证密码
        String dbPass = user.getPassword();
        String saltDB = user.getSalt();
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        if(!calcPass.equals(dbPass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        return true;
    }
}
