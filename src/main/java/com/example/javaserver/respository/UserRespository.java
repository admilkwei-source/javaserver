package com.example.javaserver.respository;


import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
// MyBatis相关导入
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.javaserver.model.dto.RegisterDTO;
import com.example.javaserver.model.entity.User;

@Mapper
// mybatis写法
public interface UserRespository {
    @Select("select * from user where id = #{id}")
    public Optional<User> searchUserById(int id);

    @Select("select * from user where username = #{username}")
    public Optional<User> searchUserByUsername(String username);

    @Insert("insert into user(username,password,nickname,create_time,update_time) values(#{username},#{password},COALESCE(#{nickname}, ''),NOW(),NOW())")
    public int insertUser(RegisterDTO registerParams);
}

