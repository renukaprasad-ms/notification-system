package com.renuka.notification_backend.auth.dto;

public class ForgotPasswordOtpResponse {

    private final String otp;

    public ForgotPasswordOtpResponse(String otp) {
        this.otp = otp;
    }

    public String getOtp() {
        return otp;
    }
}
