package com.spring.dubbyserver.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false, unique = true, length = 128)
    private String deviceId;

    @Column(length = 30)
    private String nickname;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "timezone_changed_at")
    private Instant timezoneChangedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> prefs = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @Builder
    private User(String deviceId, String nickname, String locale, String timezone,
                 Platform platform, String appVersion) {
        this.id = UUID.randomUUID();
        this.deviceId = deviceId;
        this.nickname = nickname;
        this.locale = locale != null ? locale : "ko";
        this.timezone = timezone != null ? timezone : "Asia/Seoul";
        this.prefs = new HashMap<>();
        this.status = UserStatus.ACTIVE;
        this.platform = platform;
        this.appVersion = appVersion;
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastActiveAt = now;
    }

    public void touch(String appVersion) {
        this.lastActiveAt = Instant.now();
        if (appVersion != null) {
            this.appVersion = appVersion;
        }
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeTimezone(String timezone) {
        this.timezone = timezone;
        this.timezoneChangedAt = Instant.now();
    }

    public void mergePrefs(Map<String, Object> patch) {
        if (this.prefs == null) {
            this.prefs = new HashMap<>();
        }
        patch.forEach((k, v) -> {
            if (v == null) {
                this.prefs.remove(k);
            } else {
                this.prefs.put(k, v);
            }
        });
    }

    public void markDeleting() {
        this.status = UserStatus.DELETING;
    }

    public enum UserStatus { ACTIVE, DELETING }

    public enum Platform { IOS, ANDROID }
}
