package com.example.javaserver.model.vo;

import lombok.Data;

@Data
public class UserVo {
    private Integer id;
    private String username;
    private String nickname;
    private String accessToken;
}
