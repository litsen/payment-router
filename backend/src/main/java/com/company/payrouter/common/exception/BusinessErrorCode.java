package com.company.payrouter.common.exception;

public enum BusinessErrorCode {
    BAD_REQUEST(400, 400, "Bad request"),
    UNAUTHORIZED(401, 401, "Unauthorized"),
    TOO_MANY_REQUESTS(429, 429, "Too many requests"),
    INTERNAL_ERROR(500, 500, "Internal server error"),
    INVALID_APP_ID(1001, 401, "Invalid appId"),
    INVALID_SIGN(1002, 401, "Invalid sign"),
    REQUEST_EXPIRED(1003, 400, "Request timestamp expired"),
    DUPLICATE_NONCE(1004, 400, "Duplicate nonce"),
    UNSUPPORTED_PAY_METHOD(1101, 400, "Unsupported payment method"),
    PAY_METHOD_DISABLED(1102, 400, "Payment method is disabled"),
    ORDER_ALREADY_FAILED(1201, 400, "Order already failed"),
    ORDER_NOT_FOUND(1202, 404, "Order does not exist"),
    ORDER_NOT_REFUNDABLE(1203, 400, "Only successful orders can be refunded"),
    INVALID_REQUEST_PARAMETER(1204, 400, "Invalid request parameter"),
    NO_AVAILABLE_ACCOUNT(1301, 400, "No available merchant account"),
    ROUTE_FAILED(1302, 400, "Route failed"),
    CHANNEL_ERROR(1401, 502, "Channel error"),
    INVALID_NOTIFY(1501, 400, "Invalid notify request"),
    INVALID_NOTIFY_SIGN(1502, 401, "Invalid notify sign");

    private final int code;
    private final int httpStatus;
    private final String message;

    BusinessErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
