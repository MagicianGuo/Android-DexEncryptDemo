package com.magicianguo.encryptionlib;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {
    private static final String ENCRYPT_ALGORITHM = "AES";
    private static final String CIPHER_MODE = "AES/ECB/PKCS5Padding";

    public static final byte[] BYTES = {0x02, 0x03, 0x05, 0x07, 0x0B, 0x0D, 0x11, 0x13, 0x17, 0x1D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public static byte[] encrypt(byte[] clearTextBytes, byte[] pwdBytes) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(pwdBytes, ENCRYPT_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(clearTextBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(byte[] cipherTextBytes, byte[] pwdBytes) {
        try {
            // 1 获取解密密钥
            SecretKeySpec keySpec = new SecretKeySpec(pwdBytes, ENCRYPT_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] clearTextBytes = cipher.doFinal(cipherTextBytes);
            return clearTextBytes;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
