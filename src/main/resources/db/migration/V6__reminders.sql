-- Browser push subscriptions: one per device a user turned reminders on for
CREATE TABLE IF NOT EXISTS push_subscription (
    subscription_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh VARCHAR(200) NOT NULL,
    auth VARCHAR(100) NOT NULL,
    user_agent VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_success_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_push_subscription_user ON push_subscription(user_id);

-- One row per scheduled dose that was reminded about or logged
CREATE TABLE IF NOT EXISTS dose_log (
    dose_id SERIAL PRIMARY KEY,
    prescription_id INTEGER NOT NULL REFERENCES prescription(prescription_id) ON DELETE CASCADE,
    scheduled_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,          -- PENDING, TAKEN, SKIPPED
    status_at TIMESTAMPTZ,                -- when it was marked taken/skipped
    notified_at TIMESTAMPTZ,
    followed_up_at TIMESTAMPTZ,
    UNIQUE (prescription_id, scheduled_at)
);
CREATE INDEX IF NOT EXISTS idx_dose_log_pending ON dose_log(status, scheduled_at);

-- Server-generated settings that must survive restarts (the VAPID push keys)
CREATE TABLE IF NOT EXISTS app_setting (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL
);
