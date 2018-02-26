package com.jerio.miaosha.dao;

import com.jerio.miaosha.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * Created by Jerio on 2018/2/26.
 */
@Mapper
public interface UserDao {
    @Select("select * from user where id = #{id}")
    User getUserById(int id);
}
