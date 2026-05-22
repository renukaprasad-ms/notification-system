package com.renuka.notification_backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean status;

    @JsonProperty("status_code")
    private final int statusCode;

    private final T data;
    private final String message;

    @JsonProperty("error_message")
    private final String errorMessage;

    private ApiResponse(boolean status, int statusCode, T data, String message, String errorMessage) {
        this.status = status;
        this.statusCode = statusCode;
        this.data = data;
        this.message = message;
        this.errorMessage = errorMessage;
    }

    public static <T> ApiResponse<T> success(int statusCode, T data, String message) {
        return new ApiResponse<>(true, statusCode, data, message, null);
    }

    public static ApiResponse<Void> success(int statusCode, String message) {
        return new ApiResponse<>(true, statusCode, null, message, null);
    }

    public static ApiResponse<Void> error(int statusCode, String errorMessage) {
        return new ApiResponse<>(false, statusCode, null, null, errorMessage);
    }

    public boolean isStatus() {
        return status;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
