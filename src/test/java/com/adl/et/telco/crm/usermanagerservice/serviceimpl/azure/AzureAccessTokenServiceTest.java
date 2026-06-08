package com.adl.et.telco.crm.usermanagerservice.serviceimpl.azure;

import com.adl.et.telco.crm.usermanagerservice.client.AzureClient;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.TokenResponse;
import com.adl.et.telco.crm.usermanagerservice.repository.azurerepositories.AccessTokenRepository;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureAccessTokenServiceTest {

    @Mock
    private AccessTokenRepository accessTokenRepository;

    @Mock
    private AzureClient azureClient;

    @InjectMocks
    private AzureAccessTokenService azureAccessTokenService;

    private TokenResponse tokenResponse;
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    private static final int EXPIRES_IN = 3600;

    @BeforeEach
    void setUp() {
        tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);
        tokenResponse.setExpiresIn(EXPIRES_IN);
        tokenResponse.setExtExpiresIn(3600);
        tokenResponse.setTokenType("Bearer");
    }

    @Test
    void generateAccessToken_shouldReturnTokenResponse_whenSuccessful() throws BaseException {
        // Given
        when(azureClient.getAzureToken()).thenReturn(tokenResponse);
        doNothing().when(accessTokenRepository).saveAccessToken(ACCESS_TOKEN, EXPIRES_IN);

        // When
        TokenResponse result = azureAccessTokenService.generateAccessToken();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.getExpiresIn()).isEqualTo(EXPIRES_IN);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExtExpiresIn()).isEqualTo(3600);

        verify(azureClient, times(1)).getAzureToken();
        verify(accessTokenRepository, times(1)).saveAccessToken(ACCESS_TOKEN, EXPIRES_IN);
    }

    @Test
    void generateAccessToken_shouldRethrowBaseException_whenAzureClientThrowsBaseException() throws BaseException {
        // Given
        BaseException originalException = new BaseException(
                "Azure authentication failed",
                "Authentication error",
                HttpStatus.UNAUTHORIZED,
                "401",
                null
        );

        doAnswer(invocation -> {
            throw originalException;
        }).when(azureClient).getAzureToken();

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.generateAccessToken())
                .isInstanceOf(BaseException.class)
                .hasMessage("Azure authentication failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(baseException.getResultCode()).isEqualTo("401");
                    assertThat(baseException.getReason()).isEqualTo("Authentication error");
                    assertThat(baseException.getStackTraceElements()).isNull();
                });

        verify(azureClient, times(1)).getAzureToken();
        verify(accessTokenRepository, never()).saveAccessToken(anyString(), anyInt());
    }

    @Test
    void generateAccessToken_shouldThrowBaseException_whenAzureClientThrowsRuntimeException() throws BaseException {
        // Given
        RuntimeException runtimeException = new RuntimeException("Network error");
        when(azureClient.getAzureToken()).thenThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.generateAccessToken())
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(azureClient, times(1)).getAzureToken();
        verify(accessTokenRepository, never()).saveAccessToken(anyString(), anyInt());
    }

    @Test
    void generateAccessToken_shouldThrowBaseException_whenRepositoryThrowsException() throws BaseException {
        // Given
        when(azureClient.getAzureToken()).thenReturn(tokenResponse);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(accessTokenRepository).saveAccessToken(ACCESS_TOKEN, EXPIRES_IN);

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.generateAccessToken())
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                });

        verify(azureClient, times(1)).getAzureToken();
        verify(accessTokenRepository, times(1)).saveAccessToken(ACCESS_TOKEN, EXPIRES_IN);
    }

    @Test
    void getAccessToken_shouldReturnExistingToken_whenTokenExistsInRepository() throws BaseException {
        // Given
        when(accessTokenRepository.getAccessToken()).thenReturn(ACCESS_TOKEN);

        // When
        String result = azureAccessTokenService.getAccessToken();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(ACCESS_TOKEN);

        verify(accessTokenRepository, times(1)).getAccessToken();
        verify(azureClient, never()).getAzureToken();
    }

    @Test
    void getAccessToken_shouldGenerateNewToken_whenTokenIsNull() throws BaseException {
        // Given
        when(accessTokenRepository.getAccessToken()).thenReturn(null);
        when(azureClient.getAzureToken()).thenReturn(tokenResponse);
        doNothing().when(accessTokenRepository).saveAccessToken(ACCESS_TOKEN, EXPIRES_IN);

        // When
        String result = azureAccessTokenService.getAccessToken();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(ACCESS_TOKEN);

        verify(accessTokenRepository, times(1)).getAccessToken();
        verify(azureClient, times(1)).getAzureToken();
        verify(accessTokenRepository, times(1)).saveAccessToken(ACCESS_TOKEN, EXPIRES_IN);
    }

    @Test
    void getAccessToken_shouldRethrowBaseException_whenGenerateAccessTokenThrowsBaseException() throws BaseException {
        // Given
        BaseException originalException = new BaseException(
                "Token generation failed",
                "Generation error",
                HttpStatus.SERVICE_UNAVAILABLE,
                "503",
                null
        );

        when(accessTokenRepository.getAccessToken()).thenReturn(null);
        doAnswer(invocation -> {
            throw originalException;
        }).when(azureClient).getAzureToken();

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.getAccessToken())
                .isInstanceOf(BaseException.class)
                .hasMessage("Token generation failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(baseException.getResultCode()).isEqualTo("503");
                    assertThat(baseException.getReason()).isEqualTo("Generation error");
                    assertThat(baseException.getStackTraceElements()).isNull();
                });

        verify(accessTokenRepository, times(1)).getAccessToken();
        verify(azureClient, times(1)).getAzureToken();
    }

    @Test
    void getAccessToken_shouldThrowBaseException_whenRepositoryGetTokenThrowsException() throws BaseException {
        // Given
        when(accessTokenRepository.getAccessToken())
                .thenThrow(new RuntimeException("Repository connection failed"));

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.getAccessToken())
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(accessTokenRepository, times(1)).getAccessToken();
        verify(azureClient, never()).getAzureToken();
    }

    @Test
    void getAccessToken_shouldThrowBaseException_whenNullPointerExceptionOccurs() {
        // Given
        when(accessTokenRepository.getAccessToken())
                .thenThrow(new NullPointerException("Null value encountered"));

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.getAccessToken())
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                });

        verify(accessTokenRepository, times(1)).getAccessToken();
    }

    @Test
    void tokenResponse_shouldHandleAllGettersAndSetters() {
        // Given
        TokenResponse response = new TokenResponse();

        // When
        response.setAccessToken("test-token");
        response.setExpiresIn(7200);
        response.setExtExpiresIn(7200);
        response.setTokenType("Bearer");

        // Then
        assertThat(response.getAccessToken()).isEqualTo("test-token");
        assertThat(response.getExpiresIn()).isEqualTo(7200);
        assertThat(response.getExtExpiresIn()).isEqualTo(7200);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void tokenResponse_shouldTestToString() {
        // Given
        TokenResponse response = new TokenResponse();
        response.setAccessToken(ACCESS_TOKEN);
        response.setExpiresIn(EXPIRES_IN);
        response.setExtExpiresIn(3600);
        response.setTokenType("Bearer");

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("Bearer");
    }

    @Test
    void generateAccessToken_shouldHandleNullTokenResponse() throws BaseException {
        // Given
        when(azureClient.getAzureToken()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> azureAccessTokenService.generateAccessToken())
                .isInstanceOf(BaseException.class);

        verify(azureClient, times(1)).getAzureToken();
    }
}