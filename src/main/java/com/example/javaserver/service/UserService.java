package com.example.javaserver.service;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import com.example.javaserver.model.vo.BaseVo;
import com.example.javaserver.model.vo.UserVo;
import com.example.javaserver.model.dto.RegisterDTO;
import com.example.javaserver.model.entity.User;
import com.example.javaserver.respository.UserRespository;
import com.example.javaserver.utils.JwtUtils;

// 加上这个注解，说明把这个类交给springboot管理
@Service
// 这里所有对用户的service操作
public class UserService {
    @Autowired
    UserRespository userRespository;

    @Autowired
    JwtUtils jwtUtils;

    public BaseVo<Object> register(RegisterDTO registerParams){
        Optional<User> opUser = userRespository.searchUserByUsername(registerParams.getUsername());
        if(opUser.isPresent()){
            return BaseVo.error("该用户已存在！", null);
        }else{
            int insertUserStatus = userRespository.insertUser(registerParams);
            if(insertUserStatus == 1){
                return BaseVo.success(null);
            }else{
                return BaseVo.error("注册失败，请联系管理员", null);
            }
        }
    }

    public BaseVo<UserVo> loginUser(RegisterDTO registerParams){
        Optional<User> opUser = userRespository.searchUserByUsername(registerParams.getUsername());
        if(!opUser.isPresent()){
            return BaseVo.error("用户不存在", null);
        }
        User curretUser = opUser.get();
        boolean equalUserName = curretUser.getUsername().equals(registerParams.getUsername());
        boolean equalPassword = curretUser.getPassword().equals(registerParams.getPassword());
        if(equalUserName && equalPassword){
            UserVo resultUserInfo = new UserVo();
            resultUserInfo.setId(curretUser.getId());
            resultUserInfo.setUsername(curretUser.getUsername());
            resultUserInfo.setNickname(curretUser.getNickname());
            resultUserInfo.setAccessToken(jwtUtils.getToken(curretUser.getId().toString()));
            
            return BaseVo.success(resultUserInfo);
        }else{
            return BaseVo.error("账号或密码错误", null);
        }
    }

    public BaseVo<UserVo> getUserInfo(int id){
        Optional<User> opUser = userRespository.searchUserById(id);
        if(!opUser.isPresent()){
            return BaseVo.error("用户不存在", null);
        }
        User currentUser = opUser.get();
        UserVo resultUserInfo = new UserVo();
        resultUserInfo.setId(currentUser.getId());
        resultUserInfo.setUsername(currentUser.getUsername());
        resultUserInfo.setNickname(currentUser.getNickname());
        return BaseVo.success(resultUserInfo);
    }
}
