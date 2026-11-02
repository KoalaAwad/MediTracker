package org.springbozo.meditracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends account emails. Runs async so a slow SMTP server doesn't block requests,
 * and so response times don't reveal whether an email address has an account.
 * Without MAIL_HOST the email is written to the log instead (local testing).
 */
@Service
@Slf4j
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.base-url}")
    private String baseUrl;

    public MailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerification(String to, String name, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        send(to, "Verify your MediTracker email",
                "Hi " + name + ",\n\n"
                        + "Confirm your email to finish creating your MediTracker account:\n\n"
                        + link + "\n\n"
                        + "This link expires in 24 hours. If you didn't sign up, ignore this email.");
    }

    @Async
    public void sendPasswordReset(String to, String name, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        send(to, "Reset your MediTracker password",
                "Hi " + name + ",\n\n"
                        + "Someone asked to reset the password for this account. To choose a new one, open:\n\n"
                        + link + "\n\n"
                        + "This link expires in 30 minutes. If it wasn't you, ignore this email and your password stays the same.");
    }

    private void send(String to, String subject, String body) {
        if (mailHost == null || mailHost.isBlank()) {
            log.info("MAIL_HOST not set, not sending. To: {} | Subject: {}\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.getObject().send(message);
        } catch (Exception e) {
            log.error("Failed to send '{}' email to {}: {}", subject, to, e.getMessage());
        }
    }
}
