--liquibase formatted sql
--changeset fb:0005_insert_facebook_username_avatar_cover_friendReq_birthday splitStatements:false runOnChange:true

INSERT INTO cli.action_configs (
    platform, action_type, action_provider, version, is_active,
    endpoint_url, http_method, content_type,
    headers, form_params, payload_template, extractor_rules, metadata, description
) VALUES
-- =========================================================================================
-- 1. UPLOAD_PHOTO (Tiền đề upload file ảnh lấy photo_id phục vụ đặt avatar / cover)
-- =========================================================================================
(
    'FACEBOOK', 'UPLOAD_PHOTO', 'APP_GRAPHQL', 1, true,
    'https://graph.facebook.com/me/photos', 'POST', 'multipart/form-data',
    '{
        "authorization": "OAuth {{accessToken}}",
        "User-Agent": "[FBAN/FB4A;FBAV/522.0.0.52.96;FBBV/763259015;FBDM/{density=1.5,width=540,height=960};FBLC/vi_VN;FBRV/0;FBCR/MobiFone;FBMF/Honor;FBBD/Honor;FBPN/com.facebook.katana;FBDV/BVL-AN00;FBSV/9;FBOP/1;FBCA/x86_64:arm64-v8a;]",
        "x-tigon-is-retry": "False",
        "x-fb-device-group": "3595",
        "x-meta-zca": "eyJhbmRyb2lkIjp7ImFrYSI6eyJkYXRhVG9TaWduIjoiIiwiZXJyb3JzIjpbIktFWVNUT1JFX0RJU0FCTEVEX0JZX0NPTkZJRyJdfSwiZ3BpYSI6e319fQ",
        "x-zero-eh": "2,,ATY5qyn5pgi0LPFxwgj-nj0AQ_Kv6eqmdL2VIGqwb4BIQOtJlgLIT3SqrGnRwEFhqVI",
        "x-fb-connection-type": "WIFI",
        "app-scope-id-header": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "x-fb-sim-hni": "45201",
        "x-fb-net-hni": "45201",
        "x-zero-f-device-id": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "priority": "u=3, i",
        "x-fb-connection-quality": "EXCELLENT",
        "x-fb-rmd": "fail=Server:NoUrlMap,Default:INVALID_MAP;v=;ip=;tkn=;reqTime=0;recvTime=0",
        "x-fb-http-engine": "Tigon/Liger",
        "x-fb-client-ip": "True",
        "x-fb-server-cluster": "True",
        "x-fb-friendly-name": "upload-photo",
        "x-fb-request-analytics-tags": "{\"network_tags\":{\"product\":\"350685531728\",\"retry_attempt\":\"0\"},\"application_tags\":\"PROFILE_PIC\"}"
    }'::jsonb,
    '{
        "published": "false",
        "audience_exp": "true",
        "qn": "{{sessionId}}",
        "composer_session_id": "{{sessionId}}",
        "idempotence_token": "{{sessionId}}_-1495512223_0",
        "composer_entry_point": "camera_roll",
        "locale": "vi_VN",
        "client_country_code": "VN",
        "fb_api_req_friendly_name": "upload-photo",
        "fb_api_caller_class": "MultiPhotoUploader"
    }'::jsonb,
    '{}'::jsonb,
    '{
        "success_path": "id",
        "error_path": "error.message",
        "error_fallback_path": "error.error_user_msg",
        "error_code_path": "error.code"
    }'::jsonb,
    '{}'::jsonb,
    'Cấu hình Upload Photo lấy photo_id (APP_GRAPHQL v1)'
),

-- =========================================================================================
-- 2. SET_PROFILE_PICTURE (Avatar)
-- =========================================================================================
(
    'FACEBOOK', 'SET_PROFILE_PICTURE', 'APP_GRAPHQL', 1, true,
    'https://graph.facebook.com/graphql', 'POST', 'application/x-www-form-urlencoded',
    '{
        "authorization": "OAuth {{accessToken}}",
        "User-Agent": "[FBAN/FB4A;FBAV/522.0.0.52.96;FBBV/763259015;FBDM/{density=1.5,width=540,height=960};FBLC/vi_VN;FBRV/0;FBCR/MobiFone;FBMF/Honor;FBBD/Honor;FBPN/com.facebook.katana;FBDV/BVL-AN00;FBSV/9;FBOP/1;FBCA/x86_64:arm64-v8a;]",
        "x-tigon-is-retry": "False",
        "x-fb-device-group": "3595",
        "x-meta-zca": "eyJhbmRyb2lkIjp7ImFrYSI6eyJkYXRhVG9TaWduIjoiIiwiZXJyb3JzIjpbIktFWVNUT1JFX0RJU0FCTEVEX0JZX0NPTkZJRyJdfSwiZ3BpYSI6e319fQ",
        "x-zero-eh": "2,,ATY5qyn5pgi0LPFxwgj-nj0AQ_Kv6eqmdL2VIGqwb4BIQOtJlgLIT3SqrGnRwEFhqVI",
        "x-fb-connection-type": "WIFI",
        "app-scope-id-header": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "x-fb-sim-hni": "45201",
        "x-fb-net-hni": "45201",
        "x-zero-f-device-id": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "priority": "u=3, i",
        "x-fb-connection-quality": "EXCELLENT",
        "x-fb-rmd": "fail=Server:NoUrlMap,Default:INVALID_MAP;v=;ip=;tkn=;reqTime=0;recvTime=0",
        "x-fb-http-engine": "Tigon/Liger",
        "x-fb-client-ip": "True",
        "x-fb-server-cluster": "True",
        "x-graphql-client-library": "graphservice",
        "x-fb-friendly-name": "ProfilePictureSetMutation",
        "x-fb-request-analytics-tags": "{\"network_tags\":{\"product\":\"350685531728\",\"purpose\":\"none\",\"request_category\":\"graphql\",\"retry_attempt\":\"0\"},\"application_tags\":\"graphservice\"}"
    }'::jsonb,
    '{
        "method": "post",
        "pretty": "false",
        "format": "json",
        "server_timestamps": "true",
        "locale": "vi_VN",
        "fb_api_req_friendly_name": "ProfilePictureSetMutation",
        "fb_api_caller_class": "graphservice",
        "client_doc_id": "18097093386404904121556732537",
        "fb_api_client_context": "{\"is_background\":false}",
        "variables": "{{PAYLOAD_VARIABLES}}",
        "fb_api_analytics_tags": "[\"visitation_id=null\",\"session_id={{sessionId}}\",\"GraphServices\"]",
        "client_trace_id": "{{clientTraceId}}"
    }'::jsonb,
    '{
        "input": {
            "suppress_stories": false,
            "set_profile_photo_shield": "TURN_OFF",
            "scaled_crop_rect": {
                "y": 0,
                "width": 1,
                "x": 0,
                "height": 1
            },
            "composer_session_id": "{{composerSessionId}}",
            "profile_pic_source": "UNKNOWN",
            "client_mutation_id": "{{clientMutationId}}",
            "profile_id": "{{userId}}",
            "has_umg": false,
            "existing_photo_id": "{{photoId}}",
            "frame_entrypoint": "camera_roll",
            "profile_pic_method": "camera_roll",
            "photo_id_for_media_effects": null,
            "actor_id": "{{userId}}"
        }
    }'::jsonb,
    '{
        "success_path": "data.profile_picture_set.profile_id",
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    '{}'::jsonb,
    'Cấu hình đặt ảnh đại diện Facebook (Avatar) qua App GraphQL (v1)'
),

-- =========================================================================================
-- 3. SET_COVER_PHOTO (Cover Photo)
-- =========================================================================================
(
    'FACEBOOK', 'SET_COVER_PHOTO', 'APP_GRAPHQL', 1, true,
    'https://b-graph.facebook.com', 'POST', 'application/x-www-form-urlencoded',
    '{
        "authorization": "OAuth {{accessToken}}",
        "User-Agent": "[FBAN/FB4A;FBAV/522.0.0.52.96;FBBV/763259015;FBDM/{density=1.5,width=540,height=960};FBLC/vi_VN;FBRV/0;FBCR/MobiFone;FBMF/Honor;FBBD/Honor;FBPN/com.facebook.katana;FBDV/BVL-AN00;FBSV/9;FBOP/1;FBCA/x86_64:arm64-v8a;]",
        "x-tigon-is-retry": "False",
        "x-fb-device-group": "3595",
        "x-meta-zca": "eyJhbmRyb2lkIjp7ImFrYSI6eyJkYXRhVG9TaWduIjoiIiwiZXJyb3JzIjpbIktFWVNUT1JFX0RJU0FCTEVEX0JZX0NPTkZJRyJdfSwiZ3BpYSI6e319fQ",
        "x-zero-eh": "2,,ATYLKAmcCBhOnNLuRE65NZAu7bDDIk2kv8nMhVrEHd59m4oMlf-eQsegJkSdA-cNNoQ",
        "x-fb-connection-type": "WIFI",
        "app-scope-id-header": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "x-fb-sim-hni": "45201",
        "x-fb-net-hni": "45201",
        "x-zero-f-device-id": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "priority": "u=3, i",
        "x-fb-connection-quality": "EXCELLENT",
        "x-fb-rmd": "fail=Server:NoUrlMap,Default:INVALID_MAP;v=;ip=;tkn=;reqTime=0;recvTime=0",
        "x-fb-http-engine": "Tigon/Liger",
        "x-fb-client-ip": "True",
        "x-fb-server-cluster": "True",
        "x-fb-friendly-name": "single_photo_publish",
        "x-fb-conn-uuid-client": "{{connUuid}}"
    }'::jsonb,
    '{
        "batch": "{{PAYLOAD_VARIABLES}}",
        "fb_api_caller_class": "PhotoPublisher",
        "fb_api_req_friendly_name": "single_photo_publish",
        "include_headers": "false",
        "decode_body_json": "false",
        "streamable_json_response": "true",
        "locale": "vi_VN",
        "client_country_code": "VN"
    }'::jsonb,
    '[
        {
            "method": "POST",
            "body": "qn={{sessionId}}&photo={{photoId}}&focus_x=0.5&focus_y=0.4986111&cover_photo_type=PHOTO&cover_video_type=SLIDESHOW&no_feed_story=false&locale=vi_VN&client_country_code=VN&fb_api_req_friendly_name=publish-photo",
            "name": "publish",
            "omit_response_on_success": false,
            "relative_url": "{{userId}}/cover"
        }
    ]'::jsonb,
    '{
        "success_path": "[0][1].body.id",
        "error_path": "[0][1].body.error.message",
        "error_fallback_path": "[0][0].code",
        "error_code_path": "[0][1].body.error.code"
    }'::jsonb,
    '{}'::jsonb,
    'Cấu hình đặt ảnh bìa Facebook (Cover Photo) qua App Batch Graph API (v1)'
),

-- =========================================================================================
-- 4. CHANGE_NAME (Username / Name Change)
-- =========================================================================================
(
    'FACEBOOK', 'CHANGE_NAME', 'APP_GRAPHQL', 1, true,
    'https://graph.facebook.com/graphql', 'POST', 'application/x-www-form-urlencoded',
    '{
        "authorization": "OAuth {{accessToken}}",
        "User-Agent": "[FBAN/FB4A;FBAV/522.0.0.52.96;FBBV/763259015;FBDM/{density=1.5,width=540,height=960};FBLC/vi_VN;FBRV/0;FBCR/MobiFone;FBMF/Honor;FBBD/Honor;FBPN/com.facebook.katana;FBDV/BVL-AN00;FBSV/9;FBOP/1;FBCA/x86_64:arm64-v8a;]",
        "x-tigon-is-retry": "False",
        "x-fb-device-group": "3595",
        "x-meta-zca": "eyJhbmRyb2lkIjp7ImFrYSI6eyJkYXRhVG9TaWduIjoiIiwiZXJyb3JzIjpbIktFWVNUT1JFX0RJU0FCTEVEX0JZX0NPTkZJRyJdfSwiZ3BpYSI6e319fQ",
        "x-zero-eh": "2,,ATY5qyn5pgi0LPFxwgj-nj0AQ_Kv6eqmdL2VIGqwb4BIQOtJlgLIT3SqrGnRwEFhqVI",
        "x-fb-connection-type": "WIFI",
        "app-scope-id-header": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "x-fb-sim-hni": "45201",
        "x-fb-net-hni": "45201",
        "x-zero-f-device-id": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "priority": "u=0",
        "x-fb-connection-quality": "EXCELLENT",
        "x-fb-rmd": "fail=Server:NoUrlMap,Default:INVALID_MAP;v=;ip=;tkn=;reqTime=0;recvTime=0",
        "x-fb-http-engine": "Tigon/Liger",
        "x-fb-client-ip": "True",
        "x-fb-server-cluster": "True",
        "x-graphql-client-library": "graphservice",
        "x-graphql-request-purpose": "fetch",
        "x-fb-friendly-name": "FbBloksActionRootQuery-com.bloks.www.fxim.settings.name.change.async",
        "x-fb-request-analytics-tags": "{\"network_tags\":{\"product\":\"350685531728\",\"purpose\":\"fetch\",\"request_category\":\"graphql\",\"retry_attempt\":\"0\"},\"application_tags\":\"graphservice\"}"
    }'::jsonb,
    '{
        "method": "post",
        "pretty": "false",
        "format": "json",
        "server_timestamps": "true",
        "locale": "vi_VN",
        "purpose": "fetch",
        "fb_api_req_friendly_name": "FbBloksActionRootQuery-com.bloks.www.fxim.settings.name.change.async",
        "fb_api_caller_class": "graphservice",
        "client_doc_id": "11994080423550821853202892098",
        "fb_api_client_context": "{\"is_background\":false}",
        "variables": "{{PAYLOAD_VARIABLES}}",
        "fb_api_analytics_tags": "[\"GraphServices\"]",
        "client_trace_id": "{{clientTraceId}}"
    }'::jsonb,
    '{
        "params": {
            "params": "{\"params\":\"{\\\"client_input_params\\\":{\\\"last_name\\\":\\\"{{lastName}}\\\",\\\"full_name\\\":\\\"{{fullName}}\\\",\\\"family_device_id\\\":\\\"{{deviceId}}\\\",\\\"middle_name\\\":\\\"{{middleName}}\\\",\\\"first_name\\\":\\\"{{firstName}}\\\"},\\\"server_params\\\":{\\\"identity_identifiers\\\":[],\\\"INTERNAL__latency_qpl_marker_id\\\":36707139,\\\"identity_ids_DEPRECATED\\\":\\\"{{userId}}\\\",\\\"should_dismiss_screen_after-save\\\":0,\\\"is_meta_verified_profile_editing_flow\\\":0,\\\"requested_screen_component_type\\\":2,\\\"machine_id\\\":null,\\\"INTERNAL__latency_qpl_instance_id\\\":{{latencyQplInstanceId}}}}\"}",
            "bloks_versioning_id": "782b2f0b7d1d2eb3b26b8789268654986f45ce0c1fe66806b0060eabab018545",
            "app_id": "com.bloks.www.fxim.settings.name.change.async"
        },
        "scale": "1.5",
        "nt_context": {
            "using_white_navbar": true,
            "styles_id": "51609fb901550a6035225e028a3e4fc3",
            "pixel_ratio": 1.5,
            "is_push_on": true,
            "debug_tooling_metadata_token": null,
            "is_flipper_enabled": false,
            "theme_params": [
                {
                    "value": ["BLUEPRINT_TEST_GUTTER", "BLUEPRINT_TEST_ROUNDED_CORNERS_NO_GUTTERS"],
                    "design_system_name": "FDS"
                }
            ],
            "bloks_version": "782b2f0b7d1d2eb3b26b8789268654986f45ce0c1fe66806b0060eabab018545"
        }
    }'::jsonb,
    '{
        "success_path": "data.fb_bloks_action_root_query",
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    '{}'::jsonb,
    'Cấu hình đổi tên hiển thị (Username / Display Name) Facebook qua Bloks Query (v1)'
),

-- =========================================================================================
-- 5. SEND_FRIEND_REQUEST (Friend Request)
-- =========================================================================================
(
    'FACEBOOK', 'SEND_FRIEND_REQUEST', 'APP_GRAPHQL', 1, true,
    'https://graph.facebook.com/graphql', 'POST', 'application/x-www-form-urlencoded',
    '{
        "authorization": "OAuth {{accessToken}}",
        "User-Agent": "[FBAN/FB4A;FBAV/522.0.0.52.96;FBBV/763259015;FBDM/{density=1.5,width=540,height=960};FBLC/vi_VN;FBRV/0;FBCR/MobiFone;FBMF/Honor;FBBD/Honor;FBPN/com.facebook.katana;FBDV/BVL-AN00;FBSV/9;FBOP/1;FBCA/x86_64:arm64-v8a;]",
        "x-tigon-is-retry": "False",
        "x-fb-device-group": "3595",
        "x-meta-zca": "eyJhbmRyb2lkIjp7ImFrYSI6eyJkYXRhVG9TaWduIjoiIiwiZXJyb3JzIjpbIktFWVNUT1JFX0RJU0FCTEVEX0JZX0NPTkZJRyJdfSwiZ3BpYSI6e319fQ",
        "x-zero-eh": "2,,ATY5qyn5pgi0LPFxwgj-nj0AQ_Kv6eqmdL2VIGqwb4BIQOtJlgLIT3SqrGnRwEFhqVI",
        "x-fb-connection-type": "WIFI",
        "app-scope-id-header": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "x-fb-sim-hni": "45201",
        "x-fb-net-hni": "45201",
        "x-zero-f-device-id": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "priority": "u=3, i",
        "x-fb-connection-quality": "EXCELLENT",
        "x-fb-rmd": "fail=Server:NoUrlMap,Default:INVALID_MAP;v=;ip=;tkn=;reqTime=0;recvTime=0",
        "x-fb-http-engine": "Tigon/Liger",
        "x-fb-client-ip": "True",
        "x-fb-server-cluster": "True",
        "x-fb-friendly-name": "FriendRequestSendCoreMutation",
        "x-graphql-client-library": "graphservice",
        "x-dsp-correlation-id": "{{dspCorrelationId}}",
        "x-fb-request-analytics-tags": "{\"network_tags\":{\"product\":\"350685531728\",\"purpose\":\"none\",\"request_category\":\"graphql\",\"retry_attempt\":\"0\"},\"application_tags\":\"graphservice\"}",
        "x-fb-conn-uuid-client": "{{connUuid}}"
    }'::jsonb,
    '{
        "method": "post",
        "pretty": "false",
        "format": "json",
        "server_timestamps": "true",
        "locale": "vi_VN",
        "fb_api_req_friendly_name": "FriendRequestSendCoreMutation",
        "fb_api_caller_class": "graphservice",
        "client_doc_id": "82682510714372387825118196594",
        "fb_api_client_context": "{\"is_background\":false}",
        "variables": "{{PAYLOAD_VARIABLES}}",
        "fb_api_analytics_tags": "[\"visitation_id=null\",\"session_id={{sessionId}}\",\"GraphServices\"]",
        "client_trace_id": "{{clientTraceId}}"
    }'::jsonb,
    '{
        "input": {
            "origin": "UNKNOWN",
            "friending_channel": "SEARCH",
            "friend_requestee_ids": ["{{targetUserId}}"],
            "click_proof_validation_result": "{\"validated\":false,\"components\":[{\"class_name\":\"DecorView\",\"name\":\"DecorView\",\"is_enabled\":true,\"is_selected\":false,\"x_pos\":0,\"y_pos\":0,\"width\":540,\"height\":960,\"alpha\":1,\"is_leaf\":false},{\"class_name\":\"RCTextView\",\"name\":\"RCTextView:add_friend_button_{{targetUserId}}\",\"id\":\"add_friend_button_{{targetUserId}}\",\"is_enabled\":true,\"is_selected\":false,\"x_pos\":126,\"y_pos\":291,\"width\":213,\"height\":28,\"alpha\":1,\"is_leaf\":true,\"parent.class_name\":\"56T\"}],\"failed_steps\":[{\"step\":\"SIZE\",\"failed_on\":{\"class_name\":\"RCTextView\",\"name\":\"RCTextView:add_friend_button_{{targetUserId}}\",\"id\":\"add_friend_button_{{targetUserId}}\",\"is_enabled\":true,\"is_selected\":false,\"x_pos\":126,\"y_pos\":291,\"width\":213,\"height\":28,\"alpha\":1,\"is_leaf\":true,\"parent.class_name\":\"56T\"}}]}",
            "extra_data": {},
            "attribution_id_v2": "SearchResultsFragment,graph_search_results_page_blended,tap_search_result,{{timestamp}}.495,104238573,,,,{{timestamp}}.495"
        }
    }'::jsonb,
    '{
        "success_path": "data.friend_request_send.friendship_status",
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    '{}'::jsonb,
    'Cấu hình gửi lời mời kết bạn (Friend Request) qua App GraphQL (v1)'
),

-- =========================================================================================
-- 6. UPDATE_BIRTHDAY (Birthday)
-- =========================================================================================
(
    'FACEBOOK', 'UPDATE_BIRTHDAY', 'APP_GRAPHQL', 1, true,
    'https://graph.facebook.com/graphql', 'POST', 'application/x-www-form-urlencoded',
    '{
        "authorization": "OAuth {{accessToken}}",
        "User-Agent": "[FBAN/FB4A;FBAV/522.0.0.52.96;FBBV/763259015;FBDM/{density=1.5,width=540,height=960};FBLC/vi_VN;FBRV/0;FBCR/MobiFone;FBMF/Honor;FBBD/Honor;FBPN/com.facebook.katana;FBDV/BVL-AN00;FBSV/9;FBOP/1;FBCA/x86_64:arm64-v8a;]",
        "x-tigon-is-retry": "False",
        "x-fb-device-group": "3595",
        "x-meta-zca": "eyJhbmRyb2lkIjp7ImFrYSI6eyJkYXRhVG9TaWduIjoiIiwiZXJyb3JzIjpbIktFWVNUT1JFX0RJU0FCTEVEX0JZX0NPTkZJRyJdfSwiZ3BpYSI6e319fQ",
        "x-zero-eh": "2,,ATY5qyn5pgi0LPFxwgj-nj0AQ_Kv6eqmdL2VIGqwb4BIQOtJlgLIT3SqrGnRwEFhqVI",
        "x-fb-connection-type": "WIFI",
        "app-scope-id-header": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "x-fb-sim-hni": "45201",
        "x-fb-net-hni": "45201",
        "x-zero-f-device-id": "d0db7ed7-f05d-4ab4-a34b-27ea5eb599f1",
        "priority": "u=3, i",
        "x-fb-connection-quality": "EXCELLENT",
        "x-fb-rmd": "fail=Server:NoUrlMap,Default:INVALID_MAP;v=;ip=;tkn=;reqTime=0;recvTime=0",
        "x-fb-http-engine": "Tigon/Liger",
        "x-fb-client-ip": "True",
        "x-fb-server-cluster": "True",
        "x-fb-friendly-name": "FbBloksActionRootQuery-com.bloks.www.fxcal.xplat.settings.edit.birthday.async",
        "x-fb-request-analytics-tags": "{\"network_tags\":{\"product\":\"350685531728\",\"purpose\":\"fetch\",\"request_category\":\"graphql\",\"retry_attempt\":\"0\"},\"application_tags\":\"graphservice\"}",
        "x-fb-conn-uuid-client": "{{connUuid}}"
    }'::jsonb,
    '{
        "method": "post",
        "pretty": "false",
        "format": "json",
        "server_timestamps": "true",
        "locale": "vi_VN",
        "purpose": "fetch",
        "fb_api_req_friendly_name": "FbBloksActionRootQuery-com.bloks.www.fxcal.xplat.settings.edit.birthday.async",
        "fb_api_caller_class": "graphservice",
        "client_doc_id": "11994080423550821853202892098",
        "fb_api_client_context": "{\"is_background\":false}",
        "variables": "{{PAYLOAD_VARIABLES}}",
        "fb_api_analytics_tags": "[\"GraphServices\"]",
        "client_trace_id": "{{clientTraceId}}"
    }'::jsonb,
    '{
        "params": {
            "params": "{\"params\":\"{\\\"client_input_params\\\":{\\\"none_of_these_selected\\\":0,\\\"timestamp\\\":\\\"{{birthdayTimestamp}}\\\"},\\\"server_params\\\":{\\\"from_other_entrypoint\\\":1,\\\"requested_screen_component_type\\\":2,\\\"machine_id\\\":\\\"4AWCaABDRqDcjK1gj3uAaiiC\\\",\\\"INTERNAL__latency_qpl_marker_id\\\":36707139,\\\"INTERNAL__latency_qpl_instance_id\\\":9.468853500015E12,\\\"dialog_type\\\":\\\"edit_birthday\\\"}}\"}",
            "bloks_versioning_id": "8d129b12c898b410587535d8971dae35526b73451a4d950f95c1eee58666db21",
            "app_id": "com.bloks.www.fxcal.xplat.settings.edit.birthday.async"
        },
        "scale": "1.5",
        "nt_context": {
            "using_white_navbar": true,
            "styles_id": "51609fb901550a6035225e028a3e4fc3",
            "pixel_ratio": 1.5,
            "is_push_on": true,
            "debug_tooling_metadata_token": null,
            "is_flipper_enabled": false,
            "theme_params": [
                {
                    "value": ["BLUEPRINT_TEST_GUTTER", "BLUEPRINT_TEST_ROUNDED_CORNERS_NO_GUTTERS"],
                    "design_system_name": "FDS"
                }
            ],
            "bloks_version": "8d129b12c898b410587535d8971dae35526b73451a4d950f95c1eee58666db21"
        }
    }'::jsonb,
    '{
        "success_path": "data.fb_bloks_action_root_query",
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    '{}'::jsonb,
    'Cấu hình cập nhật ngày sinh (Birthday) Facebook qua Bloks Query (v1)'
)
ON CONFLICT (platform, action_type, action_provider, version) DO UPDATE SET
    endpoint_url = EXCLUDED.endpoint_url,
    http_method = EXCLUDED.http_method,
    content_type = EXCLUDED.content_type,
    headers = EXCLUDED.headers,
    form_params = EXCLUDED.form_params,
    payload_template = EXCLUDED.payload_template,
    extractor_rules = EXCLUDED.extractor_rules,
    metadata = EXCLUDED.metadata,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;
