-- ═══════════════════════════════════════════════════════════════
-- 더비(Dubby) V1 초기 스키마 — derby_system_spec_v1.md §7.2
-- ═══════════════════════════════════════════════════════════════

-- ═══ 사용자 ═══
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id           VARCHAR(128) NOT NULL UNIQUE,
    nickname            VARCHAR(30),                    -- NULL 허용. 표시 폴백 "사용자님". 길이 검증은 서버(yml)
    locale              VARCHAR(10)  NOT NULL DEFAULT 'ko',
    timezone            VARCHAR(50)  NOT NULL DEFAULT 'Asia/Seoul',
    timezone_changed_at TIMESTAMPTZ,
    prefs               JSONB        NOT NULL DEFAULT '{}',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DELETING')),
    platform            VARCHAR(10)  NOT NULL CHECK (platform IN ('IOS','ANDROID')),
    app_version         VARCHAR(20),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_active_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ═══ 템플릿 (업무/푸시/홈상태 공용 원장) ═══
CREATE TABLE templates (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code               VARCHAR(32)  NOT NULL UNIQUE,
    type               VARCHAR(16)  NOT NULL CHECK (type IN ('DAILY_TASK','PUSH','HOME_STATUS')),
    category           VARCHAR(32)  NOT NULL,
    time_window        VARCHAR(16)  NOT NULL DEFAULT 'ANY' CHECK (time_window IN ('ANY','MORNING','LUNCH','EVENING')),
    intensity          VARCHAR(8)   NOT NULL DEFAULT 'LOW' CHECK (intensity IN ('LOW','MID','HIGH')),
    requires_user_name BOOLEAN      NOT NULL DEFAULT FALSE,
    is_premium         BOOLEAN      NOT NULL DEFAULT FALSE,
    cooldown_days      SMALLINT     NOT NULL,
    locale             VARCHAR(8)   NOT NULL DEFAULT 'ko',
    tags               TEXT[]       NOT NULL DEFAULT '{}',
    content            JSONB        NOT NULL,
    status             VARCHAR(8)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETIRED')),
    content_version    INT          NOT NULL DEFAULT 1,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_templates_pick ON templates (type, status, locale, time_window);

-- ═══ 오늘의 업무 배정 (쿨다운 원장 겸함) ═══
CREATE TABLE daily_task_assignments (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id   BIGINT      NOT NULL REFERENCES templates(id),
    assigned_date DATE        NOT NULL,
    slot          SMALLINT    NOT NULL CHECK (slot BETWEEN 1 AND 3),
    reaction      VARCHAR(16) CHECK (reaction IN ('PRAISE','SCOLD','RETRY','IGNORE')),
    reacted_at    TIMESTAMPTZ,
    retry_count   SMALLINT    NOT NULL DEFAULT 0,
    saved         BOOLEAN     NOT NULL DEFAULT FALSE,
    shared        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, assigned_date, slot)
);
CREATE INDEX ix_dta_cooldown ON daily_task_assignments (user_id, template_id, assigned_date DESC);
CREATE INDEX ix_dta_saved ON daily_task_assignments (user_id, created_at DESC) WHERE saved;

-- ═══ 채팅 ═══
CREATE TABLE chat_messages (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role              VARCHAR(10) NOT NULL CHECK (role IN ('USER','DERBY')),
    content           TEXT        NOT NULL,
    model             VARCHAR(80),
    misread_level     SMALLINT,
    prompt_tokens     INT,
    completion_tokens INT,
    client_msg_id     VARCHAR(64),
    is_saved          BOOLEAN     NOT NULL DEFAULT FALSE,
    safety_flagged    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_chat_client_msg ON chat_messages (user_id, client_msg_id) WHERE client_msg_id IS NOT NULL;
CREATE INDEX ix_chat_user_created ON chat_messages (user_id, created_at DESC);

CREATE TABLE chat_daily_usage (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    used_count INT  NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, usage_date)
);

-- ═══ 일기장 ═══
CREATE TABLE diary_candidates (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_message_id BIGINT       REFERENCES chat_messages(id) ON DELETE SET NULL,
    fact              VARCHAR(200) NOT NULL,
    interpretation    VARCHAR(400) NOT NULL,
    conclusion        VARCHAR(200) NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')),
    expires_at        TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_diary_cand_user ON diary_candidates (user_id, status, created_at DESC);

CREATE TABLE diary_entries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    candidate_id   BIGINT       REFERENCES diary_candidates(id) ON DELETE SET NULL,
    fact           VARCHAR(200) NOT NULL,
    interpretation VARCHAR(400) NOT NULL,
    conclusion     VARCHAR(200) NOT NULL,
    auto_saved     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_shared      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_diary_user_created ON diary_entries (user_id, created_at DESC);

-- ═══ 푸시 ═══
CREATE TABLE push_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expo_push_token VARCHAR(200) NOT NULL UNIQUE,
    device_id       VARCHAR(128),
    platform        VARCHAR(10)  NOT NULL CHECK (platform IN ('IOS','ANDROID')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_push_tokens_user ON push_tokens (user_id) WHERE is_active;

CREATE TABLE push_settings (
    user_id         UUID     PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    enabled         BOOLEAN  NOT NULL DEFAULT TRUE,
    max_daily_count SMALLINT NOT NULL DEFAULT 1,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE push_send_logs (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id        BIGINT      NOT NULL REFERENCES templates(id),
    slot               VARCHAR(16) NOT NULL CHECK (slot IN ('MORNING','LUNCH','EVENING')),
    local_date         DATE        NOT NULL,
    title              VARCHAR(100) NOT NULL,
    body               TEXT        NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','SENT','TICKET_ERROR','DELIVERED','RECEIPT_ERROR')),
    expo_ticket_id     VARCHAR(64),
    error_code         VARCHAR(40),
    sent_at            TIMESTAMPTZ,
    receipt_checked_at TIMESTAMPTZ,
    opened_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, local_date, slot)
);
CREATE INDEX ix_psl_cooldown ON push_send_logs (user_id, template_id, local_date DESC);
CREATE INDEX ix_psl_receipt  ON push_send_logs (status, sent_at) WHERE status = 'SENT';

-- ═══ 결제 ═══
CREATE TABLE purchase_entitlements (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entitlement   VARCHAR(40)  NOT NULL,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE','EXPIRED','BILLING_ISSUE','CANCELLED')),
    product_id    VARCHAR(100),
    environment   VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION',
    will_renew    BOOLEAN      NOT NULL DEFAULT TRUE,
    purchased_at  TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,
    last_event_at TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, entitlement)
);

CREATE TABLE one_time_purchases (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id        VARCHAR(100) NOT NULL,
    rc_transaction_id VARCHAR(100) NOT NULL UNIQUE,
    effect_expires_at TIMESTAMPTZ,
    purchased_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_otp_user_effect ON one_time_purchases (user_id, effect_expires_at DESC);

-- RC 웹훅 원본. user FK 없음 — 계정 삭제 후에도 결제 감사 기록 보존
CREATE TABLE revenuecat_events (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id           VARCHAR(100) NOT NULL UNIQUE,
    event_type         VARCHAR(60)  NOT NULL,
    app_user_id        VARCHAR(100) NOT NULL,
    event_timestamp_ms BIGINT,
    payload            JSONB        NOT NULL,
    received_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ═══ LLM 사용량 ═══
CREATE TABLE llm_usage_log (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    purpose           VARCHAR(20) NOT NULL,
    model             VARCHAR(80) NOT NULL,
    prompt_version    VARCHAR(10) NOT NULL,
    misread_level     SMALLINT,
    prompt_tokens     INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    cost_usd          NUMERIC(10,6),
    latency_ms        INT,
    finish_reason     VARCHAR(20),
    safety_category   VARCHAR(20),
    validation_failed BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_used     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_llm_usage_day ON llm_usage_log (created_at);

CREATE TABLE global_llm_usage (
    usage_date     DATE PRIMARY KEY,
    total_tokens   BIGINT NOT NULL DEFAULT 0,
    total_cost_usd NUMERIC(10,4) NOT NULL DEFAULT 0
);

-- ═══ 지표 ═══
CREATE TABLE app_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     UUID,
    event_type  VARCHAR(48) NOT NULL,
    target_type VARCHAR(24),
    target_id   BIGINT,
    properties  JSONB NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_ae_type_time ON app_events (event_type, occurred_at);

CREATE TABLE user_activity_daily (
    user_id       UUID NOT NULL,
    activity_date DATE NOT NULL,
    PRIMARY KEY (user_id, activity_date)
);
