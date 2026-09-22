package com.fb.cli.domains.facebook.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionTemplateResolverTest {

    @Test
    @DisplayName("Thay thế placeholder cơ bản")
    void testBasicPlaceholder() {
        String template = "{\"text\": \"{{content}}\", \"author\": \"{{author}}\"}";
        Map<String, String> context = Map.of(
                "content", "Hello world",
                "author", "John"
        );

        String resolved = ActionTemplateResolver.resolveTemplate(template, context);
        assertEquals("{\"text\": \"Hello world\", \"author\": \"John\"}", resolved);
    }

    @Test
    @DisplayName("Thay thế JSON array dạng quoted placeholder \"{{attachments}}\"")
    void testJsonArrayPlaceholder() {
        String template = "{\"input\": {\"attachments\": \"{{attachments}}\", \"msg\": \"{{text}}\"}}";
        Map<String, String> context = Map.of(
                "attachments", "[{\"photo\":{\"id\":\"123\"}},{\"video\":{\"id\":\"456\"}}]",
                "text", "My multi media post"
        );

        String resolved = ActionTemplateResolver.resolveTemplate(template, context);
        assertEquals("{\"input\": {\"attachments\": [{\"photo\":{\"id\":\"123\"}},{\"video\":{\"id\":\"456\"}}], \"msg\": \"My multi media post\"}}", resolved);
    }

    @Test
    @DisplayName("Thay thế JSON array rỗng \"{{attachments}}\" -> []")
    void testEmptyJsonArrayPlaceholder() {
        String template = "{\"input\": {\"attachments\": \"{{attachments}}\"}}";
        Map<String, String> context = Map.of("attachments", "[]");

        String resolved = ActionTemplateResolver.resolveTemplate(template, context);
        assertEquals("{\"input\": {\"attachments\": []}}", resolved);
    }

    @Test
    @DisplayName("Thay thế JSON object dạng quoted placeholder \"{{customObject}}\"")
    void testJsonObjectPlaceholder() {
        String template = "{\"input\": {\"details\": \"{{customObject}}\"}}";
        Map<String, String> context = Map.of("customObject", "{\"key\":\"val\"}");

        String resolved = ActionTemplateResolver.resolveTemplate(template, context);
        assertEquals("{\"input\": {\"details\": {\"key\":\"val\"}}}", resolved);
    }
}
