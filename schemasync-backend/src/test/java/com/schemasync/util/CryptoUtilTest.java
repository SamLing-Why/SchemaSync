package com.schemasync.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptoUtil单元测试
 * 
 * @author SchemaSync Team
 * @since 2026-04-26
 */
class CryptoUtilTest {

    @Test
    void testEncryptDecrypt() {
        String originalText = "mySecretPassword123";
        
        // 加密
        String encrypted = CryptoUtil.encrypt(originalText);
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        
        // 解密
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncryptDifferentInputs() {
        // 不同的输入应该产生不同的加密结果
        String text1 = "password1";
        String text2 = "password2";
        
        String encrypted1 = CryptoUtil.encrypt(text1);
        String encrypted2 = CryptoUtil.encrypt(text2);
        
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void testEncryptSameInputConsistency() {
        // 相同的输入在相同的密钥下应该产生相同的加密结果
        String text = "testPassword";
        
        String encrypted1 = CryptoUtil.encrypt(text);
        String encrypted2 = CryptoUtil.encrypt(text);
        
        assertEquals(encrypted1, encrypted2);
    }

    @Test
    void testDecryptInvalidInput() {
        // 解密无效的输入应该抛出异常
        assertThrows(Exception.class, () -> {
            CryptoUtil.decrypt("invalidBase64!!!");
        });
    }

    @Test
    void testEncryptEmptyString() {
        String empty = "";
        String encrypted = CryptoUtil.encrypt(empty);
        assertNotNull(encrypted);
        
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(empty, decrypted);
    }

    @Test
    void testEncryptSpecialCharacters() {
        String special = "!@#$%^&*()_+-=[]{}|;':\",.<>?/~`";
        
        String encrypted = CryptoUtil.encrypt(special);
        assertNotNull(encrypted);
        
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(special, decrypted);
    }

    @Test
    void testEncryptChineseCharacters() {
        String chinese = "密码测试123";
        
        String encrypted = CryptoUtil.encrypt(chinese);
        assertNotNull(encrypted);
        
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(chinese, decrypted);
    }

    @Test
    void testEncryptLongText() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("a");
        }
        
        String encrypted = CryptoUtil.encrypt(longText.toString());
        assertNotNull(encrypted);
        
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(longText.toString(), decrypted);
    }

    @Test
    void testIsEncrypted() {
        String plainText = "plainPassword";
        String encrypted = CryptoUtil.encrypt(plainText);
        
        // 注意: 当前CryptoUtil没有isEncrypted方法,这个测试是预留的
        // 如果需要,可以在CryptoUtil中添加该方法
        // assertFalse(CryptoUtil.isEncrypted(plainText));
        // assertTrue(CryptoUtil.isEncrypted(encrypted));
    }
}
