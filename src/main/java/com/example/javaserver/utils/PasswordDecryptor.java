package com.example.javaserver.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 密码解密工具类
 * 用于解密前端通过 crypto-js 加密的密码数据
 * 
 * 加密算法：AES-CBC
 * 前端加密库：crypto-js
 */
@Component
public class PasswordDecryptor {
    
    // AES 加密算法
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    
    // 加密密钥（32字节），需与前端保持一致
    private static final String ENCRYPTION_KEY = "0123456789ABCDEF0123456789ABCDEF";
    
    // 初始化向量 IV（16字节），需与前端保持一致
    private static final String IV_STRING = "1234567890123456";
    
    /**
     * 解密前端加密的密码
     * 
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的明文密码
     */
    public static String decryptPassword(String encryptedData) {
        try {
            if (encryptedData == null || encryptedData.trim().isEmpty()) {
                throw new IllegalArgumentException("加密数据不能为空");
            }
            
            System.out.println("=== 开始解密 ===");
            System.out.println("接收到的加密数据长度: " + encryptedData.length());
            System.out.println("加密数据前50个字符: " + (encryptedData.length() > 50 ? encryptedData.substring(0, 50) : encryptedData));
            
            // 使用 UTF-8 编码将密钥字符串转为字节数组
            byte[] keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            System.out.println("密钥字节长度: " + keyBytes.length);
            
            // 使用 UTF-8 编码将 IV 字符串转为字节数组
            byte[] ivBytes = IV_STRING.getBytes(StandardCharsets.UTF_8);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            System.out.println("IV字节长度: " + ivBytes.length);
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // Base64 解码加密数据
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            System.out.println("Base64解码后的字节长度: " + encryptedBytes.length);
            System.out.println("是否为16的倍数: " + (encryptedBytes.length % 16 == 0));
            
            if (encryptedBytes.length % 16 != 0) {
                throw new IllegalArgumentException(
                    String.format("加密数据长度错误！解码后字节数: %d (不是16的倍数)。" +
                        "请检查前端是否使用了正确的加密方式。", encryptedBytes.length)
                );
            }
            
            // 执行解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            System.out.println("解密成功！明文长度: " + result.length());
            System.out.println("=== 解密完成 ===");
            
            return result;
            
        } catch (IllegalArgumentException e) {
            System.err.println("参数错误: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("解密异常: " + e.getClass().getName() + " - " + e.getMessage());
            throw new RuntimeException("密码解密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取加密密钥（供前端使用）
     */
    public static String getEncryptionKey() {
        return ENCRYPTION_KEY;
    }
    
    /**
     * 获取初始化向量 IV（供前端使用）
     */
    public static String getIvString() {
        return IV_STRING;
    }
}