package com.example.javaserver.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

// entity作为和数据库表链接的入口，需要定义表明、和主键，通过JPA来操作
// JPA相关导入 - 已注释，改用MyBatis
// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.Id;
// import jakarta.persistence.Table;
import lombok.Data;

// JPA注解 - 已注释，改用MyBatis
// @Entity
// @Table(name="user")

// mybatis-plus写法
@TableName("user")
@Data 
public class User {
    // 那个字段是主键，就写在对应字段上方
    // @Id - JPA注解，已注释
    @TableId //mybatis主键注解
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    @TableField //mybatis 时间字段（自动生成）注解
    private String create_time;
    @TableField //mybatis 时间字段（自动生成）注解
    private String update_time;
}
