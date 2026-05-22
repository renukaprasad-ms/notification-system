package com.renuka.notification_backend.auth.otp;

public interface OtpDeliveryService {

    void sendOtp(String destination, OtpPurpose purpose, String otp);
}
