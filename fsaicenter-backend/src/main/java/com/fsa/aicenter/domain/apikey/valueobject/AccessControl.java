package com.fsa.aicenter.domain.apikey.valueobject;

import com.fsa.aicenter.domain.model.valueobject.ModelType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 访问控制值对象（不可变）
 */
public class AccessControl {
    private final Set<ModelType> allowedModelTypes;
    private final Set<String> allowedIpWhitelist;

    /**
     * 私有构造器，进行防御性复制
     */
    private AccessControl(Set<ModelType> allowedModelTypes, Set<String> allowedIpWhitelist) {
        this.allowedModelTypes = allowedModelTypes == null ?
            Collections.emptySet() :
            Collections.unmodifiableSet(new HashSet<>(allowedModelTypes));
        this.allowedIpWhitelist = allowedIpWhitelist == null ?
            Collections.emptySet() :
            Collections.unmodifiableSet(new HashSet<>(allowedIpWhitelist));
    }

    public Set<ModelType> getAllowedModelTypes() {
        return allowedModelTypes;
    }

    public Set<String> getAllowedIpWhitelist() {
        return allowedIpWhitelist;
    }

    /**
     * 从Set集合创建
     */
    public static AccessControl of(Set<ModelType> allowedModelTypes, Set<String> allowedIpWhitelist) {
        return new AccessControl(allowedModelTypes, allowedIpWhitelist);
    }

    /**
     * 从逗号分隔的字符串创建
     */
    public static AccessControl fromStrings(String modelTypesStr, String ipWhitelistStr) {
        Set<ModelType> modelTypes = parseModelTypes(modelTypesStr);
        Set<String> ipWhitelist = parseIpWhitelist(ipWhitelistStr);
        return new AccessControl(modelTypes, ipWhitelist);
    }

    /**
     * 是否可访问指定模型类型
     */
    public boolean canAccessModelType(ModelType modelType) {
        if (modelType == null) {
            return false;
        }
        // 如果allowedModelTypes为空或null，表示允许所有类型
        if (allowedModelTypes == null || allowedModelTypes.isEmpty()) {
            return true;
        }
        return allowedModelTypes.contains(modelType);
    }

    /**
     * 是否允许指定IP访问
     */
    public boolean isIpAllowed(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        // 如果白名单为空或null，表示允许所有IP
        if (allowedIpWhitelist == null || allowedIpWhitelist.isEmpty()) {
            return true;
        }
        return allowedIpWhitelist.contains(ip.trim());
    }

    // 私有辅助方法
    private static Set<ModelType> parseModelTypes(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<ModelType> types = new HashSet<>();
        for (String code : str.split(",")) {
            String trimmed = code.trim();
            if (!trimmed.isEmpty()) {
                types.add(ModelType.fromCode(trimmed));
            }
        }
        return Collections.unmodifiableSet(types);
    }

    private static Set<String> parseIpWhitelist(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> ips = new HashSet<>();
        for (String ip : str.split(",")) {
            String trimmed = ip.trim();
            if (!trimmed.isEmpty()) {
                ips.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(ips);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessControl that = (AccessControl) o;
        return java.util.Objects.equals(allowedModelTypes, that.allowedModelTypes) &&
               java.util.Objects.equals(allowedIpWhitelist, that.allowedIpWhitelist);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(allowedModelTypes, allowedIpWhitelist);
    }

    @Override
    public String toString() {
        return "AccessControl(allowedModelTypes=" + allowedModelTypes +
               ", allowedIpWhitelist=" + allowedIpWhitelist + ")";
    }
}
