import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class EncryptDecrypt {
    private Cipher cipher;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public EncryptDecrypt(PublicKey publicKey,PrivateKey privateKey ) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.cipher = Cipher.getInstance("RSA");
        this.publicKey = null;
        this.privateKey = null;
    }

    public String encryptMessage(String msg, PrivateKey key) {
        String encryptedMessage = null;

        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, key);
            encryptedMessage = Base64.encodeBase64String(cipher.doFinal(msg.getBytes("UTF-8")));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return encryptedMessage;
    }

    public String decryptMessage(String msg, PublicKey key) {
        String decryptedMessage = null;

        try {
            this.cipher.init(Cipher.DECRYPT_MODE, key);
           decryptedMessage  = new String(cipher.doFinal(Base64.decodeBase64(msg)), "UTF-8");
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return decryptedMessage;
    }
}
