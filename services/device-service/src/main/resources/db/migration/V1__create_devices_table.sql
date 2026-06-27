CREATE TABLE devices (
    id               UUID         PRIMARY KEY,
    category         VARCHAR(50)  NOT NULL,
    lifecycle_status VARCHAR(50)  NOT NULL,
    received_at      TIMESTAMPTZ  NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL
);
