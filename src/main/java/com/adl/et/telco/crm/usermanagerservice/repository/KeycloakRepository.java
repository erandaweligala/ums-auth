package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.model.keycloak.KeycloakAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeycloakRepository extends JpaRepository<KeycloakAuth, Long> {
    @Query(nativeQuery = true, value = "select ka.client_secret from ums_keycloak_auth ka where ka.tenant_id = :tenantId and ka.client_id = :clientId")
    Optional<String> findClientSecretByAppAndTenant(String tenantId, String clientId);
}

