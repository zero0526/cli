package com.fb.cli.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "action_configs", schema = "cli")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ActionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "platform", length = 64, nullable = false)
    private String platform;

    @Column(name = "action_type", length = 64, nullable = false)
    private String actionType;

    @Column(name = "action_provider", length = 64, nullable = false)
    private String actionProvider;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "endpoint_url", columnDefinition = "text", nullable = false)
    private String endpointUrl;

    @Builder.Default
    @Column(name = "http_method", length = 16, nullable = false)
    private String httpMethod = "POST";

    @Builder.Default
    @Column(name = "content_type", length = 64, nullable = false)
    private String contentType = "application/x-www-form-urlencoded";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    @Builder.Default
    private String headers = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_params", columnDefinition = "jsonb")
    @Builder.Default
    private String formParams = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_template", columnDefinition = "jsonb")
    @Builder.Default
    private String payloadTemplate = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extractor_rules", columnDefinition = "jsonb")
    @Builder.Default
    private String extractorRules = "{}";

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private String metadata = "{}";

    @com.fasterxml.jackson.annotation.JsonIgnore
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
