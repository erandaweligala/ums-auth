package com.adl.et.telco.crm.usermanagerservice.client;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.user.UserAuthDTO;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyCloakClient {

    private final RestTemplate restTemplate;
    private final ResponseHandler responseHandler;
    private final ObjectMapper objectMapper = new ObjectMapper(); // You can inject it as a bean too

    @Value("${keycloak.access-token-url}")
    private String accessTokenUrl;

    @Value("${keycloak.realm-url}")
    private String realmUrl;

    /**
     * Exchanges an authorization code for an access token and extracts preferred_username from the token.
     */
    public CommonAdaptorResp<String> getKeyCloakUserDetails(String clientSecret, UserAuthDTO userAuthDTO) throws BaseException {
        log.info("Initiating Keycloak token exchange for user authentication.");

        try {
            String url = buildAccessTokenUrl(userAuthDTO.getTenant());

            String body = buildAuthorizationCodeBody(userAuthDTO, clientSecret);
            HttpEntity<String> request = new HttpEntity<>(body, getFormHeaders());

            log.debug("Requesting Keycloak token endpoint: {}", url);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, getGenericMapType());

            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null && responseBody.containsKey("access_token")) {
                String accessToken = (String) responseBody.get("access_token");

                String preferredUsername = extractPreferredUsername(accessToken);
                log.info("Successfully extracted preferred_username: {}", preferredUsername);

                return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), preferredUsername);
            }

            throw new BaseException("Access token not found", ResponseCodeEnum.DATA_NOT_FOUND.description(), HttpStatus.UNAUTHORIZED, ResponseCodeEnum.DATA_NOT_FOUND.code(), null);

        } catch (HttpStatusCodeException e) {
            log.error("HTTP error during Keycloak token request: {}", e.getResponseBodyAsString(), e);
            throw wrapHttpException(e);
        } catch (Exception e) {
            log.error("Unexpected error during Keycloak token exchange: {}", e.getMessage(), e);
            throw wrapGenericException(e);
        }
    }

    /**
     * Obtains admin-level token using client credentials.
     */
    public String getAdminAccessToken(String clientId, String clientSecret, String tenantId) {
        log.info("Requesting admin access token from Keycloak.");
        try {
            String url = buildAccessTokenUrl(tenantId);
            String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
            HttpEntity<String> request = new HttpEntity<>(body, getFormHeaders());

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, getGenericMapType());

            if (response.getStatusCode().is2xxSuccessful()) {
                String token = (String) response.getBody().get("access_token");
                log.info("Admin access token retrieved successfully.");
                return token;
            } else {
                throw new RuntimeException("Failed to retrieve admin token from Keycloak.");
            }
        } catch (Exception e) {
            log.error("Error getting admin token from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Searches for a Keycloak user by email using admin token.
     */
    public CommonAdaptorResp<Map<String, Object>> getKeyCloakUserByEmail(String email, String adminAccessToken, String tenantId) throws BaseException {
        log.info("Searching Keycloak user by email: {}", email);

        try {
            String url = UriComponentsBuilder
                    .fromUriString(realmUrl + "/users")
                    .queryParam("email", email)
                    .buildAndExpand(tenantId)
                    .toUriString();

            HttpEntity<Void> entity = new HttpEntity<>(getBearerHeaders(adminAccessToken));

            log.debug("Sending request to Keycloak user search URL: {}", url);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> user = (Map<String, Object>) response.getBody().get(0);
                log.info("User found in Keycloak for email: {}", email);

                return responseHandler.responseBuilder(
                        ResponseCodeEnum.SUCCESSFUL.code(),
                        ResponseCodeEnum.SUCCESSFUL.description(),
                        user
                );
            }

            log.warn("No Keycloak user found for email: {}", email);
            return responseHandler.responseBuilder(ResponseCodeEnum.DATA_NOT_FOUND.code(), "No user found with the given email.", null);

        } catch (HttpStatusCodeException e) {
            log.error("HTTP error while fetching user by email: {}", e.getResponseBodyAsString(), e);
            throw wrapHttpException(e);
        } catch (Exception e) {
            log.error("Unexpected error during user lookup by email: {}", e.getMessage(), e);
            throw wrapGenericException(e);
        }
    }

    // --- Helper Methods ---

    private HttpHeaders getFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private HttpHeaders getBearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String buildAccessTokenUrl(String tenantId) {
        return UriComponentsBuilder.fromUriString(accessTokenUrl)
                .buildAndExpand(Map.of("tenant_id", tenantId))
                .toString();
    }

    private String buildAuthorizationCodeBody(UserAuthDTO dto, String clientSecret) {
        return "grant_type=authorization_code" +
                "&code=" + dto.getCode() +
                "&redirect_uri=" + dto.getRedirectUri() +
                "&client_id=" + dto.getClientId() +
                "&client_secret=" + clientSecret;
    }

    private String extractPreferredUsername(String jwtToken) throws Exception {
        String[] tokenParts = jwtToken.split("\\.");
        if (tokenParts.length < 2) throw new IllegalArgumentException("Invalid JWT format");

        String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
        Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);

        return (String) payloadMap.get("preferred_username");
    }

    private ParameterizedTypeReference<Map<String, Object>> getGenericMapType() {
        return new ParameterizedTypeReference<>() {};
    }

    private BaseException wrapHttpException(HttpStatusCodeException e) {
        return new BaseException(
                e.getMessage(),
                ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.description(),
                HttpStatus.valueOf(e.getStatusCode().value()),
                ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.code(),
                e.getStackTrace()
        );
    }

    private BaseException wrapGenericException(Exception e) {
        return new BaseException(
                e.getMessage(),
                ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.description(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.code(),
                e.getStackTrace()
        );
    }
}

