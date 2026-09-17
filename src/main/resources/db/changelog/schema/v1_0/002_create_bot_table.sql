--liquibase formatted sql
--changeset fb:002_create_bot_table splitStatements:false
DO $$ BEGIN
    CREATE TYPE cli.bot_status AS ENUM (
        'ACTIVE',
        'BANNED',
        'PENDING',
        'ERROR',
        'VERIFING'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS cli.bots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bot_id varchar(128) UNIQUE,                -- ID trên platform
    bot_name varchar(255),
    platform varchar(128) NOT NULL,
    status cli.bot_status DEFAULT 'ACTIVE',
    proxy_url varchar(255),
    phone varchar(20),
    email varchar(255),
    password text,
    cookies text,
    token text,
    refresh_token text,
    token_expires_at timestamptz,
    session_data jsonb,
    user_agent text,
    settings jsonb DEFAULT '{}',
    metadata jsonb DEFAULT '{}',
    last_login_at timestamptz,
    last_error text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    last_modified_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bots_platform ON cli.bots(platform);
CREATE INDEX IF NOT EXISTS idx_bots_user_id ON cli.bots(bot_id);