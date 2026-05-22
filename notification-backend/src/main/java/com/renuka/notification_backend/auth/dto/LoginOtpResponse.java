package com.renuka.notification_backend.auth.dto;

public class LoginOtpResponse {

    private final String otp;

    public LoginOtpResponse(String otp) {
        this.otp = otp;
    }

    public String getOtp() {
        return otp;
    }
}
