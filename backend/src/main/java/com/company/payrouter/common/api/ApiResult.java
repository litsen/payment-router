package com.company.payrouter.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unified API response")
public record ApiResult<T>(
        @Schema(description = "Business status code", example = "0")
        int code,
        @Schema(description = "Response message", example = "success")
        String message,
        @Schema(description = "Response payload")
        T data
) {
    public static final int SUCCESS_CODE = 0;
    public static final int ERROR_CODE = 500;

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(SUCCESS_CODE, "success", data);
    }

    public static <T> ApiResult<T> failure(String message) {
        return failure(ERROR_CODE, message);
    }

    public static <T> ApiResult<T> failure(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
