--liquibase formatted sql
--changeset fb:001_insert_tmproxy splitStatements:false
INSERT INTO cli.proxy_providers (id, name, strategy, proxy_type, base_url, config_json, is_active, created_at, updated_at)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'TMProxy',
    'TM_Strategy',
    'ROTATING',
    'https://tmproxy.com/api/proxy',
    '{
        "code": {
            "SUCCESS": 0,
            "API_KEY_COOLDOWN": 5,
            "API_KEY_EXPIRED": 1
        },
        "endpoints": {
            "GET_NEW_PROXY": {
                "endpoint_name": "GET_NEW_PROXY",
                "method": "POST",
                "path": "/get-new-proxy",
                "timeout_ms": 15000,
                "headers": {
                    "Content-Type": "application/json",
                    "accept": "application/json"
                },
                "body_template": "{\"api_key\": \"{{api_key}}\", \"id_location\": {{id_location}}, \"id_isp\": {{id_isp}}}",
                "response_mapping": {
                    "root_path": "data",
                    "raw_proxy": "https",
                    "raw_proxy_format": "HOST_PORT",
                    "expire_at": "timeout"
                }
            },
            "GET_CURRENT_PROXY": {
                "endpoint_name": "GET_CURRENT_PROXY",
                "method": "POST",
                "path": "/get-current-proxy",
                "timeout_ms": 15000,
                "headers": {
                    "Content-Type": "application/json",
                    "accept": "application/json"
                },
                "body_template": "{\"api_key\": \"{{api_key}}\"}",
                "response_mapping": {
                    "root_path": "data",
                    "raw_proxy": "https",
                    "raw_proxy_format": "HOST_PORT",
                    "expire_at": "timeout"
                }
            },
            "KEY_EXPIRED_TIME": {
                "endpoint_name": "KEY_EXPIRED_TIME",
                "method": "POST",
                "path": "/stats",
                "timeout_ms": 15000,
                "headers": {
                    "Content-Type": "application/json",
                    "accept": "application/json"
                },
                "body_template": "{\"api_key\": \"{{api_key}}\"}",
                "response_mapping": {
                    "root_path": "data",
                    "expire_at": "expired_at"
                }
            }
        }
    }'::jsonb,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (name) DO UPDATE 
SET base_url = EXCLUDED.base_url,
    config_json = EXCLUDED.config_json,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();

INSERT INTO cli.proxy_keys (id, provider_id, api_key, is_active, created_at, expired_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM cli.proxy_providers WHERE name = 'TMProxy'),
    'tm_sample_api_key_123',
    TRUE,
    NOW(),
    NOW() + INTERVAL '30 days'
)
ON CONFLICT (api_key) DO UPDATE 
SET is_active = EXCLUDED.is_active,
    expired_at = EXCLUDED.expired_at;
