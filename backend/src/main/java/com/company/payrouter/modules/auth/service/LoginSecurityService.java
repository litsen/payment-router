package com.company.payrouter.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.payrouter.common.exception.BizException;
import com.company.payrouter.modules.auth.dto.CaptchaResponse;
import com.company.payrouter.modules.auth.dto.LoginSecurityStatusResponse;
import com.company.payrouter.modules.auth.entity.SysLoginSecurity;
import com.company.payrouter.modules.auth.mapper.SysLoginSecurityMapper;
import com.company.payrouter.modules.system.entity.SysUser;
import com.company.payrouter.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class LoginSecurityService {
    public static final String LOCK_MESSAGE = "多次输入密码错误，请联系管理员";
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int LOCK_THRESHOLD = 5;
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    private final SysLoginSecurityMapper securityMapper;
    private final SysUserMapper userMapper;
    private final SecureRandom random = new SecureRandom();

    public LoginSecurityService(SysLoginSecurityMapper securityMapper, SysUserMapper userMapper) {
        this.securityMapper = securityMapper;
        this.userMapper = userMapper;
    }

    public LoginSecurityStatusResponse status(String username, String ip) {
        SysLoginSecurity security = findByUsername(username);
        boolean locked = isIpLocked(ip) || Boolean.TRUE.equals(security == null ? null : security.getUserLocked());
        boolean captchaRequired = security != null && safeFailCount(security) >= CAPTCHA_THRESHOLD && !locked;
        return new LoginSecurityStatusResponse(captchaRequired, locked);
    }

    @Transactional
    public CaptchaResponse captcha(String username) {
        SysLoginSecurity security = findByUsername(username);
        if (security == null || safeFailCount(security) < CAPTCHA_THRESHOLD || Boolean.TRUE.equals(security.getUserLocked())) {
            return new CaptchaResponse(false, null, null);
        }
        CaptchaChallenge challenge = createChallenge();
        security.setCaptchaId(UUID.randomUUID().toString().replace("-", ""));
        security.setCaptchaCode(challenge.answer());
        security.setCaptchaExpiresAt(LocalDateTime.now().plusMinutes(CAPTCHA_EXPIRE_MINUTES));
        security.setUpdatedAt(LocalDateTime.now());
        securityMapper.updateById(security);
        return new CaptchaResponse(true, security.getCaptchaId(), challenge.imageBase64());
    }

    public void assertLoginAllowed(String username, String ip) {
        if (isIpLocked(ip)) {
            throw new BizException(401, LOCK_MESSAGE);
        }
        SysLoginSecurity security = findByUsername(username);
        if (security != null && Boolean.TRUE.equals(security.getUserLocked())) {
            throw new BizException(401, LOCK_MESSAGE);
        }
    }

    public void validateCaptchaIfRequired(String username, String captchaId, String captchaCode) {
        SysLoginSecurity security = findByUsername(username);
        if (security == null || safeFailCount(security) < CAPTCHA_THRESHOLD) {
            return;
        }
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BizException(401, "请输入图片验证码");
        }
        if (!captchaId.equals(security.getCaptchaId())
                || security.getCaptchaExpiresAt() == null
                || security.getCaptchaExpiresAt().isBefore(LocalDateTime.now())
                || !captchaCode.trim().equalsIgnoreCase(security.getCaptchaCode())) {
            throw new BizException(401, "验证码错误");
        }
    }

    @Transactional
    public void recordSuccess(String username) {
        SysLoginSecurity security = findByUsername(username);
        if (security != null) {
            securityMapper.deleteById(security.getId());
        }
    }

    @Transactional(noRollbackFor = BizException.class)
    public void recordFailure(String username, String ip, SysUser user) {
        SysLoginSecurity security = findOrCreate(username);
        int failCount = safeFailCount(security) + 1;
        security.setFailCount(failCount);
        security.setLastFailIp(ip);
        security.setLastFailTime(LocalDateTime.now());
        security.setUpdatedAt(LocalDateTime.now());
        if (failCount >= LOCK_THRESHOLD) {
            security.setUserLocked(true);
            security.setIpLocked(true);
            security.setLockedIp(ip);
            security.setLockedAt(LocalDateTime.now());
            if (user != null) {
                SysUser update = new SysUser();
                update.setId(user.getId());
                update.setStatus("DISABLED");
                update.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(update);
            }
        }
        if (security.getId() == null) {
            securityMapper.insert(security);
        } else {
            securityMapper.updateById(security);
        }
        if (failCount >= LOCK_THRESHOLD) {
            throw new BizException(401, LOCK_MESSAGE);
        }
        throw new BizException(401, failCount >= CAPTCHA_THRESHOLD ? "用户名或密码错误，请输入图片验证码" : "用户名或密码错误");
    }

    @Transactional
    public void unlockUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        SysLoginSecurity security = findByUsername(user.getUsername());
        if (security != null) {
            securityMapper.deleteById(security.getId());
        }
        if (!"ENABLED".equals(user.getStatus())) {
            SysUser update = new SysUser();
            update.setId(userId);
            update.setStatus("ENABLED");
            update.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(update);
        }
    }

    @Transactional
    public void clearByUsername(String username) {
        SysLoginSecurity security = findByUsername(username);
        if (security != null) {
            securityMapper.deleteById(security.getId());
        }
    }

    public SysLoginSecurity findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return securityMapper.selectOne(new LambdaQueryWrapper<SysLoginSecurity>().eq(SysLoginSecurity::getUsername, username));
    }

    public boolean isIpLocked(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        return securityMapper.selectCount(new LambdaQueryWrapper<SysLoginSecurity>()
                .eq(SysLoginSecurity::getIpLocked, true)
                .eq(SysLoginSecurity::getLockedIp, ip)) > 0;
    }

    private SysLoginSecurity findOrCreate(String username) {
        SysLoginSecurity security = findByUsername(username);
        if (security != null) {
            return security;
        }
        security = new SysLoginSecurity();
        security.setUsername(username);
        security.setFailCount(0);
        security.setIpLocked(false);
        security.setUserLocked(false);
        security.setUpdatedAt(LocalDateTime.now());
        return security;
    }

    private int safeFailCount(SysLoginSecurity security) {
        return security.getFailCount() == null ? 0 : security.getFailCount();
    }

    private CaptchaChallenge createChallenge() {
        int left = random.nextInt(8) + 1;
        int right = random.nextInt(8) + 1;
        String expression = left + " + " + right + " = ?";
        String answer = String.valueOf(left + right);
        return new CaptchaChallenge(answer, drawCaptcha(expression));
    }

    private String drawCaptcha(String text) {
        try {
            BufferedImage image = new BufferedImage(140, 44, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(247, 250, 255));
            graphics.fillRect(0, 0, 140, 44);
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(170 + random.nextInt(45), 185 + random.nextInt(35), 205 + random.nextInt(30)));
                int y = random.nextInt(44);
                graphics.drawLine(random.nextInt(140), y, random.nextInt(140), random.nextInt(44));
            }
            graphics.setColor(new Color(34, 76, 140));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            graphics.drawString(text, 18, 30);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new BizException("生成验证码失败");
        }
    }

    private record CaptchaChallenge(String answer, String imageBase64) {
    }
}
