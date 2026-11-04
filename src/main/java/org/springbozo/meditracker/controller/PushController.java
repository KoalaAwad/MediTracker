package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.PushSubscription;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.PushSubscriptionRepository;
import org.springbozo.meditracker.service.ReminderService;
import org.springbozo.meditracker.service.UserService;
import org.springbozo.meditracker.service.WebPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Turning dose reminders on and off for a browser/device. */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private static final int MAX_DEVICES = 10;

    private final UserService userService;
    private final PushSubscriptionRepository subscriptions;
    private final WebPushService push;
    private final ReminderService reminders;

    public PushController(UserService userService, PushSubscriptionRepository subscriptions,
                          WebPushService push, ReminderService reminders) {
        this.userService = userService;
        this.subscriptions = subscriptions;
        this.push = push;
        this.reminders = reminders;
    }

    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", push.publicKey());
    }

    /** Body is the browser's PushSubscription JSON: {endpoint, keys: {p256dh, auth}}. */
    @PostMapping("/subscriptions")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> body, Authentication auth,
                                       @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String endpoint = body.get("endpoint") instanceof String s ? s : null;
        Map<?, ?> keys = body.get("keys") instanceof Map<?, ?> m ? m : Map.of();
        String p256dh = keys.get("p256dh") instanceof String s ? s : null;
        String authKey = keys.get("auth") instanceof String s ? s : null;
        if (endpoint == null || endpoint.length() > 1000 || p256dh == null || p256dh.length() > 200
                || authKey == null || authKey.length() > 100 || !WebPushService.isAllowedEndpoint(endpoint)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not a valid push subscription"));
        }
        User user = me(auth);
        // a browser keeps its endpoint across logins, so it moves to whoever is signed in now
        PushSubscription sub = subscriptions.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        if (sub.getId() == 0 && subscriptions.findByUser(user.getId()).size() >= MAX_DEVICES) {
            return ResponseEntity.status(409).body(Map.of("error", "Reminders are on for too many devices. Turn one off first."));
        }
        sub.setUser(user);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(authKey);
        sub.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(300, userAgent.length())));
        subscriptions.save(sub);
        return ResponseEntity.ok(Map.of("message", "Reminders are on for this device"));
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> body, Authentication auth) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) subscriptions.deleteByEndpointAndUser(endpoint, me(auth).getId());
        return ResponseEntity.ok(Map.of("message", "Reminders are off for this device"));
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(Authentication auth) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Reminders are working");
        payload.put("body", "You'll get a notification like this when a dose is due.");
        payload.put("tag", "test");
        payload.put("url", "/dashboard");
        int sent = reminders.sendToUser(me(auth), payload);
        if (sent == 0) {
            return ResponseEntity.status(409).body(Map.of("error", "No device with reminders turned on could be reached"));
        }
        return ResponseEntity.ok(Map.of("sent", sent));
    }

    private User me(Authentication auth) {
        return userService.findByEmail(auth.getName()).orElseThrow();
    }
}
