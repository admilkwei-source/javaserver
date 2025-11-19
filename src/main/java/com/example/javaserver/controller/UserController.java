package com.example.javaserver.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javaserver.model.dto.RegisterDTO;
import com.example.javaserver.model.vo.BaseVo;
import com.example.javaserver.model.vo.UserVo;
import com.example.javaserver.service.UserService;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;

// 所有用户相关的接口都写在这里
@RestController
@RequestMapping("/user")
public class UserController {
    // UserService userService = new UserService();

    // 在service层和repository层使用@Service和@Repository注解，springboot会自动管理这些类
    // 在controller层使用@Autowired注解，springboot会自动注入这些类
    @Autowired
    UserService userService;

    @PostMapping("/register")
    public BaseVo<Object> register(@RequestBody RegisterDTO registerParams) {
        return userService.register(registerParams);
    }

    @PostMapping("/login")
    public BaseVo<UserVo> loginUser(@RequestBody RegisterDTO registerParams, HttpSession session) {
        String sessionCode = (String) session.getAttribute("code");
        String requestCaptcha = registerParams.getCaptcha();
        if (sessionCode == null) {
            return BaseVo.error("验证码已失效，请重新获取", null);
        }
        if (requestCaptcha == null || !sessionCode.equalsIgnoreCase(requestCaptcha)) {
            return BaseVo.error("验证码错误", null);
        }
        session.removeAttribute("code");
        return userService.loginUser(registerParams);
    }

    @PostMapping("/getUserInfo")
    public BaseVo<UserVo> getUserInfo(@RequestBody RegisterDTO registerParams) {
        return userService.getUserInfo(registerParams.getId());
    }

    @GetMapping("/getCaptcha")
    public BaseVo<String> getCaptcha(@RequestParam(value = "param", required = false) String param, HttpSession session) {
        DefaultKaptcha captcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.image.width", "120");
        properties.setProperty("kaptcha.image.height", "40");
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.session.key", "code");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        captcha.setConfig(new Config(properties));

        String createText = captcha.createText();
        session.setAttribute("code", createText);
        BufferedImage image = captcha.createImage(createText);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", outputStream);
            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return BaseVo.success("data:image/jpeg;base64," + base64Image);
        } catch (IOException e) {
            throw new RuntimeException("验证码生成失败", e);
        }
    }
    
    
}
