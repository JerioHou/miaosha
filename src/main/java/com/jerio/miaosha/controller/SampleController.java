package com.jerio.miaosha.controller;

import com.jerio.miaosha.domain.User;
import com.jerio.miaosha.redis.RedisService;
import com.jerio.miaosha.redis.UserKey;
import com.jerio.miaosha.result.Result;
import com.jerio.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by Jerio on 2018/2/26.
 */
@Controller
@RequestMapping("/demo")
public class SampleController {
    @Autowired
    UserService userService;
    @Autowired
    RedisService redisService;

    @RequestMapping("/thymeleaf")
    public String  thymeleaf(Model model) {
        model.addAttribute("name", "Joshua");
        return "hello";
    }
    @RequestMapping("/get/{id}")
    @ResponseBody
    public Result<User> getUserById(@PathVariable int id){
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User  user  = redisService.get(UserKey.getById, ""+1, User.class);
        return Result.success(user);
    }

    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet() {
        User user  = new User();
        user.setId(1);
        user.setName("1111");
        redisService.set(UserKey.getById, ""+1, user);//UserKey:id1
        return Result.success(true);
    }

}
