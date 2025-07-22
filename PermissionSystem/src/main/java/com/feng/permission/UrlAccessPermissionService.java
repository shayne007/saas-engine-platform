package com.feng.permission;

import java.util.Collection;
import java.util.Set;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * TODO
 *
 * @since 2025/3/12
 */
@Service
public class UrlAccessPermissionService {
    private final UrlAccessResourceRepository urlAccessResourceRepository;
    private final AccessPermissionRepository permissionRepository;

    public UrlAccessPermissionService(UrlAccessResourceRepository urlAccessResourceRepository,
        AccessPermissionRepository permissionRepository) {
        this.urlAccessResourceRepository = urlAccessResourceRepository;
        this.permissionRepository = permissionRepository;
    }

    @Cacheable(value = "urlPermissionCache", key = "#requestUrl + #method + #authorities")
    public boolean hasPermission(String requestUrl, String method, Collection<? extends GrantedAuthority> authorities) {
        // current URL's permissions
        UrlAccessResource urlAccessResource = urlAccessResourceRepository.findByPathAndMethod(requestUrl, method)
            .orElseThrow(() -> new IllegalArgumentException("url not found"));
        Set<AccessPermission> permissions = urlAccessResource.getPermissions();

        // match user's authorities
        return permissions.stream().anyMatch(permission -> isMatch(authorities, permission));

    }

    private boolean isMatch(Collection<? extends GrantedAuthority> authorities, AccessPermission permission) {
        return authorities.stream().anyMatch(authority -> authority.getAuthority().equals(permission.getName));
    }
}
