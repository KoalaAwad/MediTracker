package org.springbozo.meditracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Sends Web Push messages with the JDK's own crypto: payload encryption per RFC 8291 (aes128gcm)
 * and server identification per RFC 8292 (VAPID). The VAPID key pair is generated once and kept
 * in the database, because changing it invalidates every browser subscription.
 */
@Slf4j
@Service
public class WebPushService {

    public enum Result { SENT, GONE, FAILED }

    // Only real push services, so a crafted "subscription" can't make the server call arbitrary URLs
    private static final List<String> PUSH_HOSTS = List.of(
            "fcm.googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com");
    private static final int RECORD_SIZE = 4096;
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final JdbcTemplate jdbc;
    private final String subject;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final SecureRandom random = new SecureRandom();
    private volatile KeyPair vapid;

    public WebPushService(JdbcTemplate jdbc, @Value("${app.push.subject:mailto:no-reply@koala.rip}") String subject) {
        this.jdbc = jdbc;
        this.subject = subject;
    }

    public static boolean isAllowedEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null && uri.getUserInfo() == null
                    && PUSH_HOSTS.stream().anyMatch(h -> host.equals(h) || host.endsWith("." + h));
        } catch (Exception e) {
            return false;
        }
    }

    /** The public key browsers need to subscribe, base64url of the uncompressed point. */
    public String publicKey() {
        return B64.encodeToString(uncompressed((ECPublicKey) vapidKeys().getPublic()));
    }

    public Result send(String endpoint, String p256dh, String auth, String json) {
        if (!isAllowedEndpoint(endpoint)) return Result.GONE;
        try {
            byte[] body = encrypt(json.getBytes(StandardCharsets.UTF_8), B64D.decode(p256dh), B64D.decode(auth));
            URI uri = URI.create(endpoint);
            String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Encoding", "aes128gcm")
                    // a dose reminder is useless a few hours late
                    .header("TTL", "7200")
                    .header("Urgency", "high")
                    .header("Authorization", "vapid t=" + vapidJwt(audience) + ", k=" + publicKey())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            if (code >= 200 && code < 300) return Result.SENT;
            if (code == 404 || code == 410) return Result.GONE;
            log.warn("Push to {} failed: HTTP {} {}", uri.getHost(), code, res.body());
            // 400/403 mean the subscription can never work (e.g. it belongs to other VAPID keys)
            return code == 400 || code == 403 ? Result.GONE : Result.FAILED;
        } catch (IllegalArgumentException e) {
            return Result.GONE; // malformed keys
        } catch (Exception e) {
            log.warn("Push failed: {}", e.toString());
            return Result.FAILED;
        }
    }

    // ---- RFC 8291 ----

    byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"), random);
        KeyPair ephemeral = gen.generateKeyPair();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return encrypt(plaintext, uaPublic, authSecret, ephemeral, salt);
    }

    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair ephemeral, byte[] salt)
            throws GeneralSecurityException {
        if (uaPublic.length != 65 || uaPublic[0] != 4 || authSecret.length != 16) {
            throw new IllegalArgumentException("Bad subscription keys");
        }
        byte[] asPublic = uncompressed((ECPublicKey) ephemeral.getPublic());

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(publicKeyFrom(uaPublic), true);
        byte[] ecdhSecret = ka.generateSecret();

        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
        byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        // single record: plaintext, then the 0x02 "last record" delimiter
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = aes.doFinal(concat(plaintext, new byte[]{2}));
        if (ciphertext.length + 86 > RECORD_SIZE) throw new IllegalArgumentException("Push payload too large");

        return ByteBuffer.allocate(16 + 4 + 1 + asPublic.length + ciphertext.length)
                .put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic).put(ciphertext)
                .array();
    }

    // HKDF-SHA256 with a single output block (all our lengths are <= 32)
    static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        mac.update(info);
        mac.update((byte) 1);
        return Arrays.copyOf(mac.doFinal(), length);
    }

    // ---- RFC 8292 ----

    String vapidJwt(String audience) throws GeneralSecurityException {
        String header = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        long exp = Instant.now().plus(Duration.ofHours(12)).getEpochSecond();
        String claims = B64.encodeToString(("{\"aud\":\"" + audience + "\",\"exp\":" + exp + ",\"sub\":\"" + subject + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        Signature sig = Signature.getInstance("SHA256withECDSAinP1363Format");
        sig.initSign(vapidKeys().getPrivate());
        sig.update((header + "." + claims).getBytes(StandardCharsets.US_ASCII));
        return header + "." + claims + "." + B64.encodeToString(sig.sign());
    }

    KeyPair vapidKeys() {
        if (vapid != null) return vapid;
        synchronized (this) {
            if (vapid != null) return vapid;
            try {
                List<String> stored = jdbc.queryForList(
                        "SELECT setting_value FROM app_setting WHERE setting_key = 'vapid.private'", String.class);
                if (stored.isEmpty()) {
                    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
                    gen.initialize(new ECGenParameterSpec("secp256r1"), random);
                    KeyPair pair = gen.generateKeyPair();
                    jdbc.update("INSERT INTO app_setting (setting_key, setting_value) VALUES ('vapid.private', ?), ('vapid.public', ?) ON CONFLICT DO NOTHING",
                            B64.encodeToString(pair.getPrivate().getEncoded()), B64.encodeToString(pair.getPublic().getEncoded()));
                    log.info("Generated VAPID keys for web push");
                    stored = jdbc.queryForList(
                            "SELECT setting_value FROM app_setting WHERE setting_key = 'vapid.private'", String.class);
                }
                String pub = jdbc.queryForObject(
                        "SELECT setting_value FROM app_setting WHERE setting_key = 'vapid.public'", String.class);
                KeyFactory kf = KeyFactory.getInstance("EC");
                vapid = new KeyPair(
                        kf.generatePublic(new X509EncodedKeySpec(B64D.decode(pub))),
                        kf.generatePrivate(new PKCS8EncodedKeySpec(B64D.decode(stored.get(0)))));
                return vapid;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Could not load VAPID keys", e);
            }
        }
    }

    // ---- EC helpers ----

    static byte[] uncompressed(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 4;
        copyFixed(key.getW().getAffineX(), out, 1);
        copyFixed(key.getW().getAffineY(), out, 33);
        return out;
    }

    static ECPublicKey publicKeyFrom(byte[] uncompressed) throws GeneralSecurityException {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec spec = params.getParameterSpec(ECParameterSpec.class);
        ECPoint point = new ECPoint(new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(uncompressed, 33, 65)));
        // the JDK checks the point is on the curve
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, spec));
    }

    private static void copyFixed(BigInteger n, byte[] out, int offset) {
        byte[] b = n.toByteArray();
        int len = Math.min(b.length, 32);
        System.arraycopy(b, b.length - len, out, offset + 32 - len, len);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) out.writeBytes(p);
        return out.toByteArray();
    }
}
