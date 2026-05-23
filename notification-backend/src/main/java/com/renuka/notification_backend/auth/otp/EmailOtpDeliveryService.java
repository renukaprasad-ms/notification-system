package com.renuka.notification_backend.auth.otp;

import com.renuka.notification_backend.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailOtpDeliveryService implements OtpDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOtpDeliveryService.class);

    private final JavaMailSender javaMailSender;
    private final String mailHost;
    private final String mailUsername;
    private final String fromAddress;

    public EmailOtpDeliveryService(
            JavaMailSender javaMailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${app.mail.from:}") String fromAddress
    ) {
        this.javaMailSender = javaMailSender;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendOtp(String destination, OtpPurpose purpose, String otp) {
        if (!isMailConfigured()) {
            LOGGER.warn("Mail is not configured. Refusing to create an unusable OTP flow for {}", destination);
            throw new BadRequestException("OTP email delivery is not configured on the server");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(destination);
        message.setSubject(subjectFor(purpose));
        message.setText(bodyFor(purpose, otp));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new BadRequestException("Unable to send OTP email");
        }
    }

    private boolean isMailConfigured() {
        return StringUtils.hasText(mailHost) && StringUtils.hasText(mailUsername);
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(fromAddress)) {
            return fromAddress.trim();
        }

        return mailUsername.trim();
    }

    private String subjectFor(OtpPurpose purpose) {
        if (purpose == OtpPurpose.PASSWORD_RESET) {
            return "Reset your password";
        }

        return "Your login OTP";
    }

    private String bodyFor(OtpPurpose purpose, String otp) {
        String action = purpose == OtpPurpose.PASSWORD_RESET ? "reset your password" : "login";

        return "Use this OTP to " + action + ": " + otp + "\n\n"
                + "This OTP expires soon. If you did not request it, ignore this email.";
    }
}
