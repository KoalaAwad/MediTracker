package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.AuthToken;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.enums.AuthTokenType;
import org.springbozo.meditracker.repository.AuthTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthTokenService {

    public static final Duration VERIFY_TTL = Duration.ofHours(24);
    public static final Duration RESET_TTL = Duration.ofMinutes(30);
    // minimum gap between two emails of the same type to the same user
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final AuthTokenRepository authTokenRepository;
    private final SecureRandom random = new SecureRandom();

    public AuthTokenService(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    public boolean recentlySent(User user, AuthTokenType type) {
        return authTokenRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                user.getId(), type, LocalDateTime.now().minus(RESEND_COOLDOWN));
    }

    /**
     * Creates a token and returns the raw value to put in the email link.
     * Any earlier unused token of the same type stops working.
     */
    @Transactional
    public String create(User user, AuthTokenType type) {
        authTokenRepository.deleteUnusedByUserIdAndType(user.getId(), type);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(LocalDateTime.now().plus(type == AuthTokenType.VERIFY_EMAIL ? VERIFY_TTL : RESET_TTL));
        authTokenRepository.save(token);
        return raw;
    }

    /**
     * Marks the token used and returns its user, or empty if it is unknown, expired or already used.
     */
    @Transactional
    public Optional<User> consume(String raw, AuthTokenType type) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        Optional<AuthToken> found = authTokenRepository.findByTokenHashAndType(hash(raw.trim()), type);
        if (found.isEmpty()) return Optional.empty();

        AuthToken token = found.get();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        token.setUsedAt(LocalDateTime.now());
        authTokenRepository.save(token);
        return Optional.of(token.getUser());
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
