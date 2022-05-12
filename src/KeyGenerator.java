import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

public class KeyGenerator {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private KeyPairGenerator keyPairGenerator;
    private KeyPair keyPair;

    public KeyGenerator(int keylen) throws NoSuchAlgorithmException, NoSuchProviderException {
        this.keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        this.keyPairGenerator.initialize(keylen);
    }

    public void createKeys() {
        this.keyPair = this.keyPairGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
