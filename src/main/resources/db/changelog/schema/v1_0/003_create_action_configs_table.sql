--liquibase formatted sql
--changeset fb:003_create_action_configs_table splitStatements:false

CREATE TABLE IF NOT EXISTS cli.action_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    action_provider VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    endpoint_url TEXT NOT NULL,
    http_method VARCHAR(16) NOT NULL DEFAULT 'POST',
    content_type VARCHAR(64) NOT NULL DEFAULT 'application/x-www-form-urlencoded',
    headers JSONB NOT NULL DEFAULT '{}',
    form_params JSONB NOT NULL DEFAULT '{}',
    payload_template JSONB NOT NULL DEFAULT '{}',
    extractor_rules JSONB NOT NULL DEFAULT '{}',
    description TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_action_config UNIQUE (platform, action_type, action_provider, version)
);

CREATE INDEX IF NOT EXISTS idx_action_lookup 
ON cli.action_configs(platform, action_type, action_provider, is_active);
