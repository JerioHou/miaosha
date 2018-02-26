package com.jerio.miaosha.service;

import com.jerio.miaosha.dao.UserDao;
import com.jerio.miaosha.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Jerio on 2018/2/26.
 */
@Service
public class UserService {
    @Autowired
    UserDao userDao;

    public User getUserById(int id){
        return userDao.getUserById(id);
    }
}
