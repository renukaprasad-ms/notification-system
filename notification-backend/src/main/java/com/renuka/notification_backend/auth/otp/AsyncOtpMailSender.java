package com.renuka.notification_backend.auth.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncOtpMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncOtpMailSender.class);

    private final JavaMailSender javaMailSender;

    public AsyncOtpMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async("mailDeliveryExecutor")
    public void send(SimpleMailMessage message) {
        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            String[] recipients = message.getTo();
            String destination = recipients != null && recipients.length > 0 ? recipients[0] : "unknown";
            LOGGER.error("Failed to send OTP email to {}", destination, exception);
        }
    }
}
