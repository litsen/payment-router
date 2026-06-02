package com.company.payrouter.common.exception;

public class BizException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public BizException(String message) {
        this(BusinessErrorCode.BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = code;
    }

    public BizException(BusinessErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public BizException(BusinessErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
        this.httpStatus = errorCode.httpStatus();
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
