package com.company.payrouter.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Page response payload")
public record PageResult<T>(
        @Schema(description = "Page records")
        List<T> records,
        @Schema(description = "Total record count", example = "0")
        long total,
        @Schema(description = "Current page number", example = "1")
        long current,
        @Schema(description = "Page size", example = "10")
        long size
) {
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records, total, current, size);
    }
}
