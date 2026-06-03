package com.company.payrouter.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.system.dto.SystemSettingDtos.AssetUploadResponse;
import com.company.payrouter.modules.system.dto.SystemSettingDtos.SystemSettingsResponse;
import com.company.payrouter.modules.system.dto.SystemSettingDtos.SystemSettingsUpdateRequest;
import com.company.payrouter.modules.system.entity.SysSetting;
import com.company.payrouter.modules.system.mapper.SysSettingMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SystemSettingService {
    private static final String SITE_NAME = "siteName";
    private static final String COPYRIGHT_TEXT = "copyrightText";
    private static final String LOGO_URL = "logoUrl";
    private static final String LOGIN_BACKGROUND_URL = "loginBackgroundUrl";
    private static final String FAVICON_URL = "faviconUrl";
    private static final Set<String> ASSET_TYPES = Set.of("logo", "loginBackground", "favicon");
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;

    private final SysSettingMapper settingMapper;
    private final Path uploadDir;

    public SystemSettingService(
            SysSettingMapper settingMapper,
            @Value("${pay-router.upload.dir:${PAY_ROUTER_UPLOAD_DIR:/app/uploads}}") String uploadDir
    ) {
        this.settingMapper = settingMapper;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public SystemSettingsResponse getSettings() {
        Map<String, String> settings = currentSettings();
        return new SystemSettingsResponse(
                settings.get(SITE_NAME),
                settings.get(COPYRIGHT_TEXT),
                settings.get(LOGO_URL),
                settings.get(LOGIN_BACKGROUND_URL),
                settings.get(FAVICON_URL)
        );
    }

    @Transactional
    public SystemSettingsResponse updateSettings(SystemSettingsUpdateRequest request) {
        upsert(SITE_NAME, request.siteName());
        upsert(COPYRIGHT_TEXT, clean(request.copyrightText()));
        upsert(LOGO_URL, clean(request.logoUrl()));
        upsert(LOGIN_BACKGROUND_URL, clean(request.loginBackgroundUrl()));
        upsert(FAVICON_URL, clean(request.faviconUrl()));
        return getSettings();
    }

    public AssetUploadResponse uploadAsset(String type, MultipartFile file) {
        if (!ASSET_TYPES.contains(type)) {
            throw new BizException("Unsupported asset type");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException("Please select an image");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BizException("Image size cannot exceed 2MB");
        }
        String extension = extension(file);
        try {
            Path targetDir = uploadDir.resolve("system").resolve(type).normalize();
            Files.createDirectories(targetDir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = targetDir.resolve(fileName).normalize();
            if (!target.startsWith(targetDir)) {
                throw new BizException("Invalid upload path");
            }
            file.transferTo(target);
            return new AssetUploadResponse("/uploads/system/" + type + "/" + fileName);
        } catch (IOException exception) {
            throw new BizException("Image upload failed");
        }
    }

    private Map<String, String> currentSettings() {
        Map<String, String> settings = defaults();
        List<SysSetting> rows = settingMapper.selectList(new LambdaQueryWrapper<SysSetting>()
                .in(SysSetting::getSettingKey, settings.keySet()));
        Map<String, String> saved = rows.stream()
                .filter(setting -> StringUtils.hasText(setting.getSettingValue()))
                .collect(Collectors.toMap(SysSetting::getSettingKey, setting -> clean(setting.getSettingValue())));
        settings.replaceAll((key, value) -> saved.getOrDefault(key, value));
        return settings;
    }

    private Map<String, String> defaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(SITE_NAME, "支付路由后台");
        defaults.put(COPYRIGHT_TEXT, "Copyright © xxx公司");
        defaults.put(LOGO_URL, "/brand/logo.png");
        defaults.put(LOGIN_BACKGROUND_URL, "/brand/login-bg.png");
        defaults.put(FAVICON_URL, "/brand/logo.png");
        return defaults;
    }

    private void upsert(String key, String value) {
        SysSetting setting = settingMapper.selectOne(new LambdaQueryWrapper<SysSetting>().eq(SysSetting::getSettingKey, key));
        if (setting == null) {
            setting = new SysSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setUpdatedAt(LocalDateTime.now());
            settingMapper.insert(setting);
            return;
        }
        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        settingMapper.updateById(setting);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String extension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/gif".equals(contentType)) {
            return ".gif";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        if ("image/x-icon".equals(contentType) || "image/vnd.microsoft.icon".equals(contentType)) {
            return ".ico";
        }
        throw new BizException("Only image files are supported");
    }
}
