package com.company.payrouter.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_login_security")
public class SysLoginSecurity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Integer failCount;
    private String captchaId;
    private String captchaCode;
    private LocalDateTime captchaExpiresAt;
    private String lastFailIp;
    private String lockedIp;
    private Boolean ipLocked;
    private Boolean userLocked;
    private LocalDateTime lastFailTime;
    private LocalDateTime lockedAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }

    public LocalDateTime getCaptchaExpiresAt() {
        return captchaExpiresAt;
    }

    public void setCaptchaExpiresAt(LocalDateTime captchaExpiresAt) {
        this.captchaExpiresAt = captchaExpiresAt;
    }

    public String getLastFailIp() {
        return lastFailIp;
    }

    public void setLastFailIp(String lastFailIp) {
        this.lastFailIp = lastFailIp;
    }

    public String getLockedIp() {
        return lockedIp;
    }

    public void setLockedIp(String lockedIp) {
        this.lockedIp = lockedIp;
    }

    public Boolean getIpLocked() {
        return ipLocked;
    }

    public void setIpLocked(Boolean ipLocked) {
        this.ipLocked = ipLocked;
    }

    public Boolean getUserLocked() {
        return userLocked;
    }

    public void setUserLocked(Boolean userLocked) {
        this.userLocked = userLocked;
    }

    public LocalDateTime getLastFailTime() {
        return lastFailTime;
    }

    public void setLastFailTime(LocalDateTime lastFailTime) {
        this.lastFailTime = lastFailTime;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
