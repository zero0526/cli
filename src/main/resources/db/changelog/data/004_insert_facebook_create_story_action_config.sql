--liquibase formatted sql
--changeset fb:004_insert_facebook_create_story_action_config splitStatements:false runOnChange:true

INSERT INTO cli.action_configs (
    platform, action_type, action_provider, version, is_active,
    endpoint_url, http_method, content_type,
    headers, form_params, payload_template, extractor_rules, metadata, description
) VALUES
(
    'FACEBOOK', 'CREATE_STORY', 'WEB_GRAPHQL', 1, true,
    'https://www.facebook.com/api/graphql/', 'POST', 'application/x-www-form-urlencoded',
    '{
        "x-fb-friendly-name": "ComposerStoryCreateMutation",
        "x-asbd-id": "359341",
        "x-fb-lsd": "{{lsdToken}}",
        "origin": "https://www.facebook.com",
        "referer": "https://www.facebook.com/",
        "sec-fetch-site": "same-origin",
        "sec-fetch-mode": "cors",
        "sec-fetch-dest": "empty"
    }'::jsonb,
    '{
        "av": "{{userId}}",
        "__aaid": "0",
        "__user": "{{userId}}",
        "__a": "1",
        "__req": "2b",
        "dpr": "1",
        "__ccg": "EXCELLENT",
        "__rev": "1047863799",
        "__comet_req": "15",
        "fb_dtsg": "{{dtsgToken}}",
        "jazoest": "{{jazoest}}",
        "lsd": "{{lsdToken}}",
        "fb_api_caller_class": "RelayModern",
        "fb_api_req_friendly_name": "ComposerStoryCreateMutation",
        "server_timestamps": "true",
        "doc_id": "28778531428503134",
        "variables": "{{PAYLOAD_VARIABLES}}"
    }'::jsonb,
    '{
        "input": {
            "composer_entry_point": "inline_composer",
            "composer_source_surface": "timeline",
            "composer_type": "profile",
            "idempotence_token": "{{idempotenceId}}_FEED",
            "source": "WWW",
            "attachments": "{{attachments}}",
            "audience": {
                "privacy": {
                    "allow": [],
                    "base_state": "{{privacy}}",
                    "deny": [],
                    "tag_expansion_state": "UNSPECIFIED"
                }
            },
            "message": {
                "ranges": [],
                "text": "{{content}}"
            },
            "with_tags_ids": null,
            "inline_activities": [],
            "text_format_preset_id": "0",
            "ai_generated_self_disclosure_metadata": {
                "was_self_disclosed_as_ai_generated": false
            },
            "publishing_flow": {
                "supported_flows": [
                    "ASYNC_SILENT",
                    "ASYNC_NOTIF",
                    "FALLBACK"
                ]
            },
            "post_publish_story_data": {
                "reshare_post_as_sticker": "DISABLED"
            },
            "logging": {
                "composer_session_id": "{{sessionId}}"
            },
            "navigation_data": {
                "attribution_id_v2": "ProfileCometTimelineListViewRoot.react,comet.profile.timeline.list,unexpected,{{timestamp}},761333,190055527696468,,;CometActivityLogMainContentRoot.react,comet.ActivityLog.CometActivityLogMainContentRoute,via_cold_start,{{timestamp}},514359,,,,"
            },
            "tracking": [
                null
            ],
            "event_share_metadata": {
                "surface": "timeline"
            },
            "actor_id": "{{userId}}",
            "client_mutation_id": "1"
        },
        "feedLocation": "TIMELINE",
        "feedbackSource": 0,
        "focusCommentID": null,
        "gridMediaWidth": 230,
        "groupID": null,
        "scale": 1,
        "privacySelectorRenderLocation": "COMET_STREAM",
        "checkPhotosToReelsUpsellEligibility": true,
        "referringStoryRenderLocation": null,
        "renderLocation": "timeline",
        "useDefaultActor": false,
        "inviteShortLinkKey": null,
        "isFeed": false,
        "isFundraiser": false,
        "isFunFactPost": false,
        "isGroup": false,
        "isEvent": false,
        "isTimeline": true,
        "isSocialLearning": false,
        "isPageNewsFeed": false,
        "isProfileReviews": false,
        "isWorkSharedDraft": false,
        "__relay_internal__pv__CometUFIShareActionMigrationrelayprovider": true,
        "__relay_internal__pv__GHLShouldChangeSponsoredDataFieldNamerelayprovider": true,
        "__relay_internal__pv__GHLShouldChangeAdIdFieldNamerelayprovider": true,
        "__relay_internal__pv__CometUFI_dedicated_comment_routable_dialog_gkrelayprovider": true,
        "__relay_internal__pv__CometUFICommentAutoTranslationTyperelayprovider": "AUTO_TRANSLATE",
        "__relay_internal__pv__CometUFICommentAvatarStickerAnimatedImagerelayprovider": false,
        "__relay_internal__pv__CometUFICommentActionLinksRewriteEnabledrelayprovider": true,
        "__relay_internal__pv__IsWorkUserrelayprovider": false,
        "__relay_internal__pv__CometUFIReactionsEnableShortNamerelayprovider": false,
        "__relay_internal__pv__CometUFISingleLineUFIrelayprovider": true,
        "__relay_internal__pv__CometFeedStory_enable_reactor_facepilerelayprovider": false,
        "__relay_internal__pv__CometFeedStory_enable_social_bubblesrelayprovider": false,
        "__relay_internal__pv__CometFeedStory_enable_post_permalink_white_space_clickrelayprovider": false,
        "__relay_internal__pv__TestPilotShouldIncludeDemoAdUseCaserelayprovider": false,
        "__relay_internal__pv__FBReels_deprecate_short_form_video_context_gkrelayprovider": true,
        "__relay_internal__pv__FBReels_enable_view_dubbed_audio_type_gkrelayprovider": true,
        "__relay_internal__pv__CometFeedShareMedia_shouldPrefetchShareImagerelayprovider": false,
        "__relay_internal__pv__CometImmersivePhotoCanUserDisable3DMotionrelayprovider": false,
        "__relay_internal__pv__WorkCometIsEmployeeGKProviderrelayprovider": false,
        "__relay_internal__pv__IsMergQAPollsrelayprovider": false,
        "__relay_internal__pv__FBReelsMediaFooter_comet_enable_reels_ads_gkrelayprovider": true,
        "__relay_internal__pv__relay_provider_comet_ufi_ssr_seo_deferrelayprovider": true,
        "__relay_internal__pv__ReelsIFUCard_reelsIFULikeCountrelayprovider": false,
        "__relay_internal__pv__FBReelsIFUTileContent_reelsIFUPlayOnHoverrelayprovider": true,
        "__relay_internal__pv__GroupsCometGYSJFeedItemHeightrelayprovider": 206,
        "__relay_internal__pv__StoriesShouldEnablePhotosensitiveContentWarningrelayprovider": false,
        "__relay_internal__pv__ShouldEnableBakedInTextStoriesrelayprovider": false,
        "__relay_internal__pv__StoriesShouldIncludeFbNotesrelayprovider": false,
        "__relay_internal__pv__groups_comet_use_glvrelayprovider": true,
        "__relay_internal__pv__GHLShouldChangeSponsoredAuctionDistanceFieldNamerelayprovider": true,
        "__relay_internal__pv__GHLShouldUseSponsoredAuctionLabelFieldNameV1relayprovider": true,
        "__relay_internal__pv__GHLShouldUseSponsoredAuctionLabelFieldNameV2relayprovider": false
    }'::jsonb,
    '{
        "success_path": "data.story_create.story.id",
        "id_extractor": {
            "path": "data.story_create.story.legacy_story_hideable_id"
        },
        "error_path": "errors[0].description",
        "error_fallback_path": "errors[0].message",
        "error_code_path": "errors[0].code"
    }'::jsonb,
    '{
        "attachment_templates": {
            "photo": {
                "photo": {
                    "id": "{{photoId}}"
                }
            },
            "video": {
                "video": {
                    "id": "{{videoId}}",
                    "audio_descriptions": null,
                    "additional_video_metadata": {
                        "translatedAudioMetadata": []
                    },
                    "notify_when_processed": true,
                    "transcriptions": null,
                    "was_created_via_unified_video_flow": {
                        "was_created_via_unified_video_flow": true
                    }
                }
            }
        }
    }'::jsonb,
    'Cấu hình Facebook Web GraphQL Create Story / Post with Flexible Photos & Videos (v1)'
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

