--liquibase formatted sql
--changeset fb:001_initial_schema splitStatements:false validCheckSum:ANY
CREATE SCHEMA IF NOT EXISTS cli;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE cli.provider_strategy AS ENUM (
        'TM_Strategy'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE cli.rotation_type AS ENUM (
        'STATIC',
        'ROTATING'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS cli.proxy_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(100) NOT NULL UNIQUE,

    strategy cli.provider_strategy NOT NULL,
    proxy_type cli.rotation_type NOT NULL,
    base_url VARCHAR(255) NOT NULL,

    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cli.proxy_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID,
    api_key VARCHAR(100) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    expired_at TIMESTAMP,

    CONSTRAINT fk_proxy_keys_provider
        FOREIGN KEY (provider_id)
        REFERENCES cli.proxy_providers(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_proxy_keys_provider_id
    ON cli.proxy_keys(provider_id);

CREATE INDEX IF NOT EXISTS idx_proxy_keys_active
    ON cli.proxy_keys(provider_id, is_active);