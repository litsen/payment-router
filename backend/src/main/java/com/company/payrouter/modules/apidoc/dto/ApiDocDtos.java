package com.company.payrouter.modules.apidoc.dto;

import java.util.List;

public final class ApiDocDtos {
    private ApiDocDtos() {
    }

    public record ApiDocSummary(
            String slug,
            String title,
            String path
    ) {
    }

    public record ApiDocResponse(
            String slug,
            String title,
            String description,
            String requestMethod,
            String requestPath,
            List<ApiDocParameter> requestParams,
            List<ApiDocParameter> responseParams,
            String request,
            String response,
            String rules,
            List<ApiDocErrorCode> errors
    ) {
    }

    public record ApiDocErrorCode(
            String code,
            String message
    ) {
    }

    public record ApiDocParameter(
            String name,
            String type,
            String required,
            String description,
            String enums,
            String example
    ) {
    }
}
