--liquibase formatted sql
--changeset fb:002_insert_facebook_action_configs splitStatements:false

INSERT INTO cli.action_configs (
    platform, action_type, action_provider, version, is_active,
    endpoint_url, http_method, content_type,
    headers, form_params, payload_template, extractor_rules, description
) VALUES
(
    'FACEBOOK', 'COMMENT', 'WEB_GRAPHQL', 1, true,
    'https://www.facebook.com/api/graphql/', 'POST', 'application/x-www-form-urlencoded',
    '{
        "x-fb-friendly-name": "useCometUFICreateCommentMutation",
        "x-asbd-id": "359341",
        "x-fb-lsd": "{{lsdToken}}",
        "origin": "https://www.facebook.com",
        "referer": "https://www.facebook.com",
        "sec-fetch-site": "same-origin",
        "sec-fetch-mode": "cors",
        "sec-fetch-dest": "empty"
    }'::jsonb,
    '{
        "av": "{{userId}}",
        "__aaid": "0",
        "__user": "{{userId}}",
        "__a": "1",
        "__req": "25",
        "dpr": "1",
        "__ccg": "EXCELLENT",
        "__rev": "1019282004",
        "__comet_req": "15",
        "fb_dtsg": "{{dtsgToken}}",
        "jazoest": "{{jazoest}}",
        "lsd": "{{lsdToken}}",
        "fb_api_caller_class": "RelayModern",
        "fb_api_req_friendly_name": "useCometUFICreateCommentMutation",
        "server_timestamps": "true",
        "doc_id": "24356199814008185",
        "variables": "{{PAYLOAD_VARIABLES}}"
    }'::jsonb,
    '{
        "feedLocation": "DEDICATED_COMMENTING_SURFACE",
        "feedbackSource": 110,
        "input": {
            "client_mutation_id": "1",
            "actor_id": "{{userId}}",
            "feedback_id": "{{base64FeedbackId}}",
            "message": {
                "ranges": [],
                "text": "{{content}}"
            },
            "session_id": "{{sessionId}}",
            "idempotence_token": "client:{{idempotenceId}}",
            "feedback_source": "DEDICATED_COMMENTING_SURFACE",
            "attribution_id_v2": "CometHomeRoot.react,comet.home,via_cold_start,{{timestamp}},267777,4748854339,,",
            "is_tracking_encrypted": true,
            "tracking": []
        },
        "useDefaultActor": false
    }'::jsonb,
    '{
        "success_path": "data.comment_create.feedback_comment_edge.node.id",
        "id_extractor": {
            "path": "data.comment_create.feedback_comment_edge.node.id",
            "decode": "BASE64",
            "regex": "comment:(\\\\d+_\\\\d+)"
        },
        "notice_path": "data.comment_create.feedback.comments_disabled_notice_renderer.notice_message.text",
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    'Cấu hình Facebook Web GraphQL Comment Mutation (default v1)'
),
(
    'FACEBOOK', 'REACTION', 'WEB_GRAPHQL', 1, true,
    'https://www.facebook.com/api/graphql/', 'POST', 'application/x-www-form-urlencoded',
    '{
        "x-fb-friendly-name": "CometUFIFeedbackReactMutation",
        "x-asbd-id": "129477",
        "x-fb-lsd": "{{lsdToken}}",
        "origin": "https://www.facebook.com",
        "referer": "https://www.facebook.com",
        "sec-fetch-site": "same-origin",
        "sec-fetch-mode": "cors",
        "sec-fetch-dest": "empty"
    }'::jsonb,
    '{
        "av": "{{userId}}",
        "__aaid": "0",
        "__user": "{{userId}}",
        "__a": "1",
        "__req": "1x",
        "dpr": "1",
        "__ccg": "GOOD",
        "__rev": "1019282004",
        "__comet_req": "15",
        "fb_dtsg": "{{dtsgToken}}",
        "jazoest": "{{jazoest}}",
        "lsd": "{{lsdToken}}",
        "fb_api_caller_class": "RelayModern",
        "fb_api_req_friendly_name": "CometUFIFeedbackReactMutation",
        "server_timestamps": "true",
        "doc_id": "8995964513767096",
        "variables": "{{PAYLOAD_VARIABLES}}"
    }'::jsonb,
    '{
        "input": {
            "attribution_id_v2": "CometSinglePostDialogRoot.react,comet.post.single_dialog,via_cold_start,{{timestamp}},270454,,,",
            "feedback_id": "{{base64FeedbackId}}",
            "feedback_reaction_id": "{{reactionId}}",
            "feedback_source": "OBJECT",
            "is_tracking_encrypted": true,
            "tracking": [],
            "session_id": "{{sessionId}}",
            "actor_id": "{{userId}}",
            "client_mutation_id": "1"
        },
        "useDefaultActor": false
    }'::jsonb,
    '{
        "success_path": "data.feedback_react.feedback.id",
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    'Cấu hình Facebook Web GraphQL Reaction Mutation (default v1)'
)
ON CONFLICT (platform, action_type, action_provider, version) DO NOTHING;
