package com.example.campusevent.common.api;

public final class ErrorCode {

    public static final int VALIDATION_ERROR = 40000;
    public static final int UNAUTHORIZED = 40100;
    public static final int FORBIDDEN = 40300;
    public static final int NOT_FOUND = 40400;
    public static final int CONFLICT = 40900;
    public static final int INTERNAL_ERROR = 50000;

    public static final int USERNAME_EXISTS = CONFLICT;
    public static final int INVALID_CREDENTIALS = VALIDATION_ERROR;
    public static final int USER_DISABLED = VALIDATION_ERROR;
    public static final int AI_RESPONSE_INVALID = INTERNAL_ERROR;
    public static final int AI_SERVICE_UNAVAILABLE = INTERNAL_ERROR;
    public static final int AI_NOT_CONFIGURED = INTERNAL_ERROR;
    public static final int AI_TIMEOUT = INTERNAL_ERROR;

    private ErrorCode() {
    }
}
