package com.adl.et.telco.crm.usermanagerservice.model.keycloak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_keycloak_auth")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private String clientId;
    private String clientSecret;
}
