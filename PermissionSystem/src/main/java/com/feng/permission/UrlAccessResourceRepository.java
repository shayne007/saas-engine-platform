package com.feng.permission;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO
 *
 * @since 2025/3/12
 */
public interface UrlAccessResourceRepository extends JpaRepository<UrlAccessResource, Long> {
    Optional<UrlAccessResource> findByPathAndMethod(String requestUrl, String method);
}
