package com.schemasync.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 加密工具类 - AES加密解密
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
public class CryptoUtil {

    private static final Logger log = LoggerFactory.getLogger(CryptoUtil.class);

    /**
     * AES加密密钥(实际项目中应该从配置文件读取)
     * AES要求密钥长度为128/192/256位(16/24/32字节)
     */
    private static final String SECRET_KEY = "SchemaSync2026!!";  // 16字节

    /**
     * 加密算法
     */
    private static final String ALGORITHM = "AES";

    /**
     * 加密
     * 
     * @param data 明文
     * @return Base64编码的密文
     */
    public static String encrypt(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     * 
     * @param encryptedData Base64编码的密文
     * @return 明文
     */
    public static String decrypt(String encryptedData) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 判断字符串是否已加密(Base64格式)
     */
    public static boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
