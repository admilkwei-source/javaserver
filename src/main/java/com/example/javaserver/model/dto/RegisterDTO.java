package com.example.javaserver.model.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String captcha;
}
