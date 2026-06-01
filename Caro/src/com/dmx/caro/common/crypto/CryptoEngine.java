package com.dmx.caro.common.crypto;

import com.dmx.caro.common.protocol.NetworkMessage;
import com.dmx.caro.common.protocol.ProtocolCodec;
import com.dmx.caro.common.protocol.ProtocolException;
import com.dmx.caro.common.util.XmlSupport;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class CryptoEngine {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String AES_ALGORITHM = "AES";

    private CryptoEngine() {
    }

    public static KeyPair generateRsaKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    public static SessionKeys generateSessionKeys() throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(128);
        SecretKey aesKey = keyGenerator.generateKey();

        byte[] hmacBytes = new byte[32];
        RANDOM.nextBytes(hmacBytes);
        SecretKey hmacKey = new SecretKeySpec(hmacBytes, HMAC_ALGORITHM);
        return new SessionKeys(aesKey, hmacKey);
    }

    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey decodePublicKey(String encoded) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static String encryptSessionKeys(SessionKeys sessionKeys, PublicKey publicKey)
            throws GeneralSecurityException {
        String payload = "aes=" + Base64.getEncoder().encodeToString(sessionKeys.aesKey().getEncoded())
                + "\nhmac=" + Base64.getEncoder().encodeToString(sessionKeys.hmacKey().getEncoded());

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static SessionKeys decryptSessionKeys(String encryptedBundle, PrivateKey privateKey)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBundle));
        String payload = new String(decrypted, StandardCharsets.UTF_8);

        String[] lines = payload.split("\\R");
        String aes = null;
        String hmac = null;
        for (String line : lines) {
            if (line.startsWith("aes=")) {
                aes = line.substring(4).trim();
            } else if (line.startsWith("hmac=")) {
                hmac = line.substring(5).trim();
            }
        }

        if (aes == null || hmac == null) {
            throw new GeneralSecurityException("Session key bundle is incomplete.");
        }

        SecretKey aesKey = new SecretKeySpec(Base64.getDecoder().decode(aes), AES_ALGORITHM);
        SecretKey hmacKey = new SecretKeySpec(Base64.getDecoder().decode(hmac), HMAC_ALGORITHM);
        return new SessionKeys(aesKey, hmacKey);
    }

    public static String encryptMessage(NetworkMessage message, SessionKeys sessionKeys)
            throws GeneralSecurityException {
        byte[] plainBytes = ProtocolCodec.encode(message).getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, sessionKeys.aesKey(), new IvParameterSpec(iv));
        byte[] cipherBytes = cipher.doFinal(plainBytes);

        byte[] hmac = computeHmac(iv, cipherBytes, sessionKeys.hmacKey());
        return buildSecureEnvelope(iv, cipherBytes, hmac);
    }

    public static NetworkMessage decryptMessage(String secureEnvelope, SessionKeys sessionKeys)
            throws GeneralSecurityException, ProtocolException {
        try {
            Document document = XmlSupport.parse(secureEnvelope);
            Element root = document.getDocumentElement();
            if (!"secure".equals(root.getTagName())) {
                throw new ProtocolException("Unexpected secure envelope root: " + root.getTagName());
            }

            byte[] iv = Base64.getDecoder().decode(root.getAttribute("iv"));
            byte[] expectedMac = Base64.getDecoder().decode(root.getAttribute("mac"));
            String cipherText = XmlSupport.childText(root, "ciphertext", "");
            byte[] cipherBytes = Base64.getDecoder().decode(cipherText);

            byte[] actualMac = computeHmac(iv, cipherBytes, sessionKeys.hmacKey());
            if (!MessageDigest.isEqual(expectedMac, actualMac)) {
                throw new GeneralSecurityException("HMAC validation failed.");
            }

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, sessionKeys.aesKey(), new IvParameterSpec(iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return ProtocolCodec.decode(new String(plainBytes, StandardCharsets.UTF_8));
        } catch (ProtocolException exception) {
            throw exception;
        } catch (GeneralSecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProtocolException("Unable to parse secure envelope: " + exception.getMessage(), exception);
        }
    }

    public static boolean looksLikeSecureEnvelope(String rawPacket) {
        return rawPacket != null && rawPacket.trim().startsWith("<secure");
    }

    private static String buildSecureEnvelope(byte[] iv, byte[] cipherBytes, byte[] hmac) {
        Document document = XmlSupport.newDocument();
        Element root = document.createElement("secure");
        root.setAttribute("iv", Base64.getEncoder().encodeToString(iv));
        root.setAttribute("mac", Base64.getEncoder().encodeToString(hmac));
        document.appendChild(root);

        Element payload = document.createElement("ciphertext");
        payload.setTextContent(Base64.getEncoder().encodeToString(cipherBytes));
        root.appendChild(payload);
        return XmlSupport.toString(document);
    }

    private static byte[] computeHmac(byte[] iv, byte[] cipherBytes, SecretKey hmacKey)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);
        mac.update(iv);
        mac.update(cipherBytes);
        return mac.doFinal();
    }
}
