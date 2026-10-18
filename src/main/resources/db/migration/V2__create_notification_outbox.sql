CREATE TABLE notification_outbox
(
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(255) NOT NULL UNIQUE,
    element_id      VARCHAR(255) NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    next_attempt_at TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ
);

CREATE INDEX idx_notification_outbox_pending
    ON notification_outbox (status, next_attempt_at, id);

CREATE INDEX idx_notification_outbox_element
    ON notification_outbox (element_id, id);