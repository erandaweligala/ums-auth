package com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication;

import com.adl.et.telco.crm.usermanagerservice.client.KeyCloakClient;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.repository.KeycloakRepository;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyCloakManagerServiceTest {

    @Mock
    private KeyCloakClient keyCloakClient;

    @Mock
    private KeycloakRepository keycloakRepository;

    @InjectMocks
    private KeyCloakManagerService keyCloakManagerService;

    private static final String EMAIL = "user@example.com";
    private static final String TENANT_ID = "tenant123";
    private static final String CLIENT_ID = "client456";
    private static final String CLIENT_SECRET = "secret789";
    private static final String ACCESS_TOKEN = "access_token_xyz";

    private Map<String, Object> userDetailMap;
    private CommonAdaptorResp<Map<String, Object>> userDetailResponse;

    @BeforeEach
    void setUp() {
        userDetailMap = new HashMap<>();
        userDetailMap.put("id", "user-id-123");
        userDetailMap.put("username", "john.doe");
        userDetailMap.put("email", EMAIL);
        userDetailMap.put("firstName", "John");
        userDetailMap.put("lastName", "Doe");
        userDetailMap.put("enabled", true);

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        userDetailResponse = CommonAdaptorResp.<Map<String, Object>>builder()
                .result(result)
                .responseData(userDetailMap)
                .build();
    }

    @Test
    void getKeyCloakUserByEmail_shouldReturnUserDetails_whenSuccessful() throws BaseException {
        // Given
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID))
                .thenReturn(ACCESS_TOKEN);
        when(keyCloakClient.getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID))
                .thenReturn(userDetailResponse);

        // When
        Map<String, Object> result = keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("id", "user-id-123");
        assertThat(result).containsEntry("username", "john.doe");
        assertThat(result).containsEntry("email", EMAIL);
        assertThat(result).containsEntry("firstName", "John");
        assertThat(result).containsEntry("lastName", "Doe");
        assertThat(result).containsEntry("enabled", true);

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID);
    }

    @Test
    void getKeyCloakUserByEmail_shouldReturnEmptyMap_whenUserDetailResponseDataIsEmpty() throws BaseException {
        // Given
        Map<String, Object> emptyMap = new HashMap<>();
        CommonAdaptorResp<Map<String, Object>> emptyResponse = CommonAdaptorResp.<Map<String, Object>>builder()
                .result(Result.builder()
                        .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                        .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                        .build())
                .responseData(emptyMap)
                .build();

        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID))
                .thenReturn(ACCESS_TOKEN);
        when(keyCloakClient.getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID))
                .thenReturn(emptyResponse);

        // When
        Map<String, Object> result = keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID);
    }

    @Test
    void getKeyCloakUserByEmail_shouldThrowBaseException_whenRepositoryThrowsException() throws BaseException {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database error");
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenThrow(repositoryException);

        // When & Then
        assertThatThrownBy(() -> keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak user fetching failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, never()).getAdminAccessToken(anyString(), anyString(), anyString());
        verify(keyCloakClient, never()).getKeyCloakUserByEmail(anyString(), anyString(), anyString());
    }

    @Test
    void getKeyCloakUserByEmail_shouldThrowBaseException_whenClientSecretNotFound() {
        // Given
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak user fetching failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, never()).getAdminAccessToken(anyString(), anyString(), anyString());
    }

    @Test
    void getKeyCloakUserByEmail_shouldThrowBaseException_whenGetAdminAccessTokenFails() throws BaseException {
        // Given
        RuntimeException tokenException = new RuntimeException("Failed to get access token");
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID))
                .thenThrow(tokenException);

        // When & Then
        assertThatThrownBy(() -> keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak user fetching failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
        verify(keyCloakClient, never()).getKeyCloakUserByEmail(anyString(), anyString(), anyString());
    }

    @Test
    void getKeyCloakUserByEmail_shouldThrowBaseException_whenGetKeyCloakUserByEmailFails() throws BaseException {
        // Given
        RuntimeException userFetchException = new RuntimeException("User not found in Keycloak");
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID))
                .thenReturn(ACCESS_TOKEN);
        when(keyCloakClient.getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID))
                .thenThrow(userFetchException);

        // When & Then
        assertThatThrownBy(() -> keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak user fetching failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID);
    }

    @Test
    void getKeyCloakUserByEmail_shouldThrowBaseException_whenNullPointerExceptionOccurs() throws BaseException {
        // Given
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID))
                .thenReturn(ACCESS_TOKEN);
        when(keyCloakClient.getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID))
                .thenThrow(new NullPointerException("Null response from Keycloak"));

        // When & Then
        assertThatThrownBy(() -> keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak user fetching failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description());
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID);
    }

    @Test
    void getKeyCloakUserByEmail_shouldHandleNullResponseData() throws BaseException {
        // Given
        CommonAdaptorResp<Map<String, Object>> nullDataResponse = CommonAdaptorResp.<Map<String, Object>>builder()
                .result(Result.builder()
                        .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                        .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                        .build())
                .responseData(null)
                .build();

        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID))
                .thenReturn(ACCESS_TOKEN);
        when(keyCloakClient.getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID))
                .thenReturn(nullDataResponse);

        // When
        Map<String, Object> result = keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID);

        // Then
        assertThat(result).isNull();

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getAdminAccessToken(CLIENT_ID, CLIENT_SECRET, TENANT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserByEmail(EMAIL, ACCESS_TOKEN, TENANT_ID);
    }

    @Test
    void getKeyCloakUserByEmail_shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Invalid email format");
        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenThrow(illegalArgumentException);

        // When & Then
        assertThatThrownBy(() -> keyCloakManagerService.getKeyCloakUserByEmail(EMAIL, TENANT_ID, CLIENT_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak user fetching failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description());
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
    }
}