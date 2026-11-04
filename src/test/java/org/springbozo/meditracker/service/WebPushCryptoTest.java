package org.springbozo.meditracker.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Decrypts what WebPushService sends the way a browser does (RFC 8291, receiver side). */
class WebPushCryptoTest {

    private static KeyPair p256() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        return gen.generateKeyPair();
    }

    /** What the browser does with the push body, using its own private key. */
    private static byte[] browserDecrypt(byte[] body, KeyPair browser, byte[] authSecret) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        buf.get(salt);
        int recordSize = buf.getInt();
        byte[] serverPublic = new byte[buf.get()];
        buf.get(serverPublic);
        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);
        assertThat(recordSize).isEqualTo(4096);

        byte[] uaPublic = WebPushService.uncompressed((ECPublicKey) browser.getPublic());
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(browser.getPrivate());
        ka.doPhase(WebPushService.publicKeyFrom(serverPublic), true);
        byte[] ecdh = ka.generateSecret();

        byte[] keyInfo = ByteBuffer.allocate(14 + 65 + 65).put("WebPush: info\0".getBytes(StandardCharsets.US_ASCII))
                .put(uaPublic).put(serverPublic).array();
        byte[] ikm = WebPushService.hkdf(authSecret, ecdh, keyInfo, 32);
        byte[] cek = WebPushService.hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = WebPushService.hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = aes.doFinal(ciphertext);
        assertThat(padded[padded.length - 1]).as("last-record delimiter").isEqualTo((byte) 2);
        return Arrays.copyOf(padded, padded.length - 1);
    }

    @Test
    void browserCanDecryptThePayload() throws Exception {
        KeyPair browser = p256();
        byte[] auth = new byte[16];
        new java.security.SecureRandom().nextBytes(auth);
        String message = "{\"title\":\"Time for Prograf 1 mg\",\"body\":\"Take 1 capsule · 08:00\"}";

        WebPushService svc = new WebPushService(null, "mailto:test@example.com");
        byte[] body = svc.encrypt(message.getBytes(StandardCharsets.UTF_8),
                WebPushService.uncompressed((ECPublicKey) browser.getPublic()), auth);

        assertThat(new String(browserDecrypt(body, browser, auth), StandardCharsets.UTF_8)).isEqualTo(message);
    }

    @Test
    void eachMessageUsesFreshKeysAndSalt() throws Exception {
        KeyPair browser = p256();
        byte[] auth = new byte[16];
        byte[] ua = WebPushService.uncompressed((ECPublicKey) browser.getPublic());
        WebPushService svc = new WebPushService(null, "mailto:test@example.com");
        byte[] a = svc.encrypt("same".getBytes(), ua, auth);
        byte[] b = svc.encrypt("same".getBytes(), ua, auth);
        assertThat(Arrays.copyOf(a, 16)).isNotEqualTo(Arrays.copyOf(b, 16));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejectsMalformedSubscriptionKeys() throws Exception {
        WebPushService svc = new WebPushService(null, "mailto:test@example.com");
        assertThrows(IllegalArgumentException.class, () -> svc.encrypt("x".getBytes(), new byte[64], new byte[16]));
        // 65 bytes but not a point on the curve
        byte[] notOnCurve = new byte[65];
        notOnCurve[0] = 4;
        notOnCurve[64] = 1;
        assertThrows(Exception.class, () -> svc.encrypt("x".getBytes(), notOnCurve, new byte[16]));
    }

    @Test
    void vapidJwtIsSignedWithThePublishedKey() throws Exception {
        KeyPair vapid = p256();
        WebPushService svc = new WebPushService(null, "mailto:test@example.com") {
            @Override
            KeyPair vapidKeys() {
                return vapid;
            }
        };
        String jwt = svc.vapidJwt("https://fcm.googleapis.com");
        String[] parts = jwt.split("\\.");
        Base64.Decoder d = Base64.getUrlDecoder();
        assertThat(new String(d.decode(parts[0]))).contains("\"alg\":\"ES256\"");
        assertThat(new String(d.decode(parts[1]))).contains("\"aud\":\"https://fcm.googleapis.com\"", "\"sub\":\"mailto:test@example.com\"");

        Signature sig = Signature.getInstance("SHA256withECDSAinP1363Format");
        sig.initVerify(WebPushService.publicKeyFrom(d.decode(svc.publicKey())));
        sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(sig.verify(d.decode(parts[2]))).isTrue();
        assertThat(d.decode(parts[2])).hasSize(64); // raw r||s, as JWS requires
    }

    @Test
    void onlyRealPushServicesAreAccepted() {
        assertThat(WebPushService.isAllowedEndpoint("https://fcm.googleapis.com/fcm/send/abc")).isTrue();
        assertThat(WebPushService.isAllowedEndpoint("https://updates.push.services.mozilla.com/wpush/v2/abc")).isTrue();
        assertThat(WebPushService.isAllowedEndpoint("https://web.push.apple.com/abc")).isTrue();
        assertThat(WebPushService.isAllowedEndpoint("https://wns2-par02p.notify.windows.com/w/?token=abc")).isTrue();

        assertThat(WebPushService.isAllowedEndpoint("http://fcm.googleapis.com/fcm/send/abc")).isFalse();
        assertThat(WebPushService.isAllowedEndpoint("https://evil.com/fcm.googleapis.com")).isFalse();
        assertThat(WebPushService.isAllowedEndpoint("https://fcm.googleapis.com.evil.com/x")).isFalse();
        assertThat(WebPushService.isAllowedEndpoint("https://user@fcm.googleapis.com/x")).isFalse();
        assertThat(WebPushService.isAllowedEndpoint("https://169.254.169.254/latest")).isFalse();
        assertThat(WebPushService.isAllowedEndpoint("not a url")).isFalse();
    }
}
