package com.example.javaserver.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.javaserver.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    JwtUtils jwtUtils;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
    throws Exception {
        String token = request.getHeader("Authorization");
        System.out.println("URI -> " + request.getRequestURI());
        if(token == null || token.equals("")){
            return writeUnauthorized(response);
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if(jwtUtils.verifyToken(token)){
            return true;
        }else{
            return writeUnauthorized(response);
        }
    }

    private boolean writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"msg\":\"登录凭证已失效，请重新登录\",\"code\":\"401\"}");
        response.getWriter().flush();
        return false;
    }

}
