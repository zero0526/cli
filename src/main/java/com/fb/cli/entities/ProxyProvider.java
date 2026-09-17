package com.fb.cli.entities;

import com.fb.cli.dtos.proxy.ProviderCodeConstant;
import com.fb.cli.dtos.proxy.ProviderConfig;
import com.fb.cli.enums.ProviderStrategy;
import com.fb.cli.enums.RotationType;
import com.fb.cli.utils.JsonUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "proxy_providers", schema = "cli")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "keys")
public class ProxyProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "strategy", nullable = false, columnDefinition = "cli.provider_strategy")
    private ProviderStrategy strategy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "proxy_type", nullable = false, columnDefinition = "cli.rotation_type")
    private RotationType proxyType;

    @Column(name = "base_url", length = 255, nullable = false)
    private String baseUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String configJson = "{}";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProxyKey> keys = new ArrayList<>();

    public void addKey(ProxyKey key) {
        keys.add(key);
        key.setProvider(this);
    }

    public void removeKey(ProxyKey key) {
        keys.remove(key);
        key.setProvider(null);
    }

    /**
     * Parse config_json thành đối tượng ProviderConfig
     */
    public ProviderConfig getParsedConfig() {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        return JsonUtils.unmarshal(configJson, ProviderConfig.class);
    }

    /**
     * Lấy mã code tương ứng với nhãn (label) từ thuộc tính 'code' trong config_json
     */
    public Integer getCode(String label) {
        ProviderConfig config = getParsedConfig();
        return config != null ? config.getCode(label) : null;
    }

    /**
     * Kiểm tra xem mã code trả về từ provider có khớp với nhãn dự kiến không
     */
    public boolean isCode(String label, Integer actualCode) {
        if (actualCode == null) return false;
        Integer expected = getCode(label);
        return actualCode.equals(expected);
    }

    /**
     * Kiểm tra mã code thành công (label: SUCCESS, mặc định 0 nếu chưa cấu hình)
     */
    public boolean isSuccess(Integer actualCode) {
        if (actualCode == null) return false;
        Integer expected = getCode(ProviderCodeConstant.SUCCESS);
        return expected != null ? actualCode.equals(expected) : actualCode == 0;
    }

    /**
     * Kiểm tra mã code cooldown/chờ đổi IP (label: API_KEY_COOLDOWN, ví dụ 5 ở TMProxy)
     */
    public boolean isCooldown(Integer actualCode) {
        return isCode(ProviderCodeConstant.API_KEY_COOLDOWN, actualCode);
    }

    /**
     * Kiểm tra mã code hết hạn API key (label: API_KEY_EXPIRED)
     */
    public boolean isExpired(Integer actualCode) {
        return isCode(ProviderCodeConstant.API_KEY_EXPIRED, actualCode);
    }
}
