package com.example.javaserver.utils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
    // 签名 -》 对于每个用户都是固定的，除非重启或特意修改，否则签名一直不变
    // 一定要作为静态属性储存，这样才能保证不会生成新的签名，无论你重新调用多少次new

    // 如何生成签名
    // 1、借助第三方库自动生成，缺陷是服务器重启后签名全部都变了，意味着所有用户都要重新登录。
    // private static Key sign = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 2、自己准备一个最少32位的字符串，任何用Keys的方法去做签名
    // 这种做法不会在服务器重启后，变更签名 
    private static String signStr = "qwertyuiop_asdfghjkl+zxcvbnm123456";
    private static Key sign = Keys.hmacShaKeyFor(signStr.getBytes(StandardCharsets.UTF_8));

    public String getToken(String userId){
        // 上面的代码是组成签名头部，接下来要组成签名荷载体
        Map<String,Object> map = new HashMap<>();
        map.put("id", userId);
        String token = Jwts.builder()
        .setClaims(map) // 传入map对象，内容自定义生成算法
        .setSubject(userId) //算法中加上唯一标识
        .setExpiration(new Date(System.currentTimeMillis() + 24 * 3600 * 100)) // 设置签名过期时间
        .signWith(sign) // 到这一步，头部、荷载的签名都加入了，也可以组成token
        .compact(); //生成token
        return token;
    }

    // 解析前端传来的token，进行解析是否有效
    public boolean verifyToken(String token){
        try{
            Jws<Claims> claims = Jwts.parserBuilder()
            .setSigningKey(sign)
            .build()
            .parseClaimsJws(token);

            Claims body = claims.getBody();
            Header head = claims.getHeader();
            System.out.println("荷载数据" + body.toString());
            System.out.println("头部数据" + head.toString());
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
