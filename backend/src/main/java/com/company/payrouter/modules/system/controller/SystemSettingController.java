package com.company.payrouter.modules.system.controller;

import com.company.payrouter.common.api.ApiResult;
import com.company.payrouter.modules.system.dto.SystemSettingDtos.AssetUploadResponse;
import com.company.payrouter.modules.system.dto.SystemSettingDtos.SystemSettingsResponse;
import com.company.payrouter.modules.system.dto.SystemSettingDtos.SystemSettingsUpdateRequest;
import com.company.payrouter.modules.system.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SystemSettingController {
    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/api/system-settings")
    public ApiResult<SystemSettingsResponse> publicSettings() {
        return ApiResult.success(settingService.getSettings());
    }

    @GetMapping("/admin/system-settings")
    public ApiResult<SystemSettingsResponse> settings() {
        return ApiResult.success(settingService.getSettings());
    }

    @PutMapping("/admin/system-settings")
    public ApiResult<SystemSettingsResponse> update(@Valid @RequestBody SystemSettingsUpdateRequest request) {
        return ApiResult.success(settingService.updateSettings(request));
    }

    @PostMapping("/admin/system-settings/assets/{type}")
    public ApiResult<AssetUploadResponse> upload(@PathVariable String type, @RequestParam("file") MultipartFile file) {
        return ApiResult.success(settingService.uploadAsset(type, file));
    }
}
