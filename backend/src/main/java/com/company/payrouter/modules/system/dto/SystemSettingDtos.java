package com.company.payrouter.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SystemSettingDtos {
    public record SystemSettingsResponse(
            String siteName,
            String copyrightText,
            String logoUrl,
            String loginBackgroundUrl,
            String faviconUrl
    ) {
    }

    public record SystemSettingsUpdateRequest(
            @NotBlank @Size(max = 64) String siteName,
            @Size(max = 128) String copyrightText,
            @Size(max = 512) String logoUrl,
            @Size(max = 512) String loginBackgroundUrl,
            @Size(max = 512) String faviconUrl
    ) {
    }

    public record AssetUploadResponse(String url) {
    }
}
