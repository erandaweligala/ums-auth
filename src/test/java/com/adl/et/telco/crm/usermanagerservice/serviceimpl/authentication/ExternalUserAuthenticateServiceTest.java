package com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication;

import com.adl.et.telco.crm.usermanagerservice.dto.authentication.ExternalUserResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
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
class ExternalUserAuthenticateServiceTest {

    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private CrmUserRepository crmUserRepository;

    @InjectMocks
    private ExternalUserAuthenticateService externalUserAuthenticateService;

    private static final String USER_NAME = "john.doe";
    private static final String PASSWORD = "encrypted_password";
    private static final String MOBILE_NUMBER = "+1234567890";
    private static final Double STATUS_ID = 1.0;
    private static final String LAST_LOGIN = "2024-02-09 10:30:00";

    private ExternalUserResponse externalUserResponse;

    @BeforeEach
    void setUp() {
        externalUserResponse = ExternalUserResponse.builder()
                .userName(USER_NAME)
                .password(PASSWORD)
                .mobileNumber(MOBILE_NUMBER)
                .statusId(STATUS_ID)
                .lastLogin(LAST_LOGIN)
                .build();
    }

    @Test
    void getExternalUserInfo_shouldReturnUserInfo_whenUserExists() throws BaseException {
        // Given
        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<ExternalUserResponse> expectedResponse = CommonAdaptorResp.<ExternalUserResponse>builder()
                .result(result)
                .responseData(externalUserResponse)
                .build();

        when(crmUserRepository.findByUserName(USER_NAME)).thenReturn(externalUserResponse);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(externalUserResponse)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<ExternalUserResponse> response =
                externalUserAuthenticateService.getExternalUserInfo(USER_NAME);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());
        assertThat(response.getResult().getResultDescription()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.description());
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getUserName()).isEqualTo(USER_NAME);
        assertThat(response.getResponseData().getPassword()).isEqualTo(PASSWORD);
        assertThat(response.getResponseData().getMobileNumber()).isEqualTo(MOBILE_NUMBER);
        assertThat(response.getResponseData().getStatusId()).isEqualTo("1");
        assertThat(response.getResponseData().getLastLogin()).isEqualTo(LAST_LOGIN);

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
        verify(responseHandler, times(1)).responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(externalUserResponse));
    }

    @Test
    void getExternalUserInfo_shouldThrowBaseException_whenUserNotFound() {
        // Given
        when(crmUserRepository.findByUserName(USER_NAME)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> externalUserAuthenticateService.getExternalUserInfo(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.INVALID_USER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.INVALID_USER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.INVALID_USER.description());
                    assertThat(baseException.getStackTraceElements()).isNull();
                });

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getExternalUserInfo_shouldRethrowBaseException_whenBaseExceptionOccurs() {
        // Given
        BaseException originalException = new BaseException(
                "Custom error message",
                "Custom reason",
                HttpStatus.UNAUTHORIZED,
                "401",
                null);

        doAnswer(invocation -> {
            throw originalException;
        }).when(crmUserRepository).findByUserName(USER_NAME);

        // When & Then
        assertThatThrownBy(() -> externalUserAuthenticateService.getExternalUserInfo(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage("Custom error message")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(baseException.getResultCode()).isEqualTo("401");
                    assertThat(baseException.getReason()).isEqualTo("Custom reason");
                    assertThat(baseException.getStackTraceElements()).isNull();
                });

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getExternalUserInfo_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database connection failed");
        when(crmUserRepository.findByUserName(USER_NAME)).thenThrow(repositoryException);

        // When & Then
        assertThatThrownBy(() -> externalUserAuthenticateService.getExternalUserInfo(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database connection failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getExternalUserInfo_shouldHandleNullPointerException() {
        // Given
        NullPointerException npe = new NullPointerException("Null value encountered");
        when(crmUserRepository.findByUserName(USER_NAME)).thenThrow(npe);

        // When & Then
        assertThatThrownBy(() -> externalUserAuthenticateService.getExternalUserInfo(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage("Null value encountered")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                });

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
    }

    @Test
    void externalUserResponse_shouldHandleNullStatusId() throws BaseException {
        // Given
        ExternalUserResponse userWithNullStatus = ExternalUserResponse.builder()
                .userName(USER_NAME)
                .password(PASSWORD)
                .mobileNumber(MOBILE_NUMBER)
                .statusId(null)
                .lastLogin(LAST_LOGIN)
                .build();

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<ExternalUserResponse> expectedResponse = CommonAdaptorResp.<ExternalUserResponse>builder()
                .result(result)
                .responseData(userWithNullStatus)
                .build();

        when(crmUserRepository.findByUserName(USER_NAME)).thenReturn(userWithNullStatus);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(userWithNullStatus)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<ExternalUserResponse> response =
                externalUserAuthenticateService.getExternalUserInfo(USER_NAME);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getStatusId()).isNull();

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
    }

    @Test
    void externalUserResponse_shouldRoundStatusId() throws BaseException {
        // Given
        ExternalUserResponse userWithDecimalStatus = ExternalUserResponse.builder()
                .userName(USER_NAME)
                .password(PASSWORD)
                .mobileNumber(MOBILE_NUMBER)
                .statusId(2.7)
                .lastLogin(LAST_LOGIN)
                .build();

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<ExternalUserResponse> expectedResponse = CommonAdaptorResp.<ExternalUserResponse>builder()
                .result(result)
                .responseData(userWithDecimalStatus)
                .build();

        when(crmUserRepository.findByUserName(USER_NAME)).thenReturn(userWithDecimalStatus);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(userWithDecimalStatus)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<ExternalUserResponse> response =
                externalUserAuthenticateService.getExternalUserInfo(USER_NAME);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getStatusId()).isEqualTo("3"); // 2.7 rounds to 3

        verify(crmUserRepository, times(1)).findByUserName(USER_NAME);
    }

    @Test
    void externalUserResponse_shouldTestAllGetters() {
        // Given
        ExternalUserResponse response = new ExternalUserResponse(
                PASSWORD,
                MOBILE_NUMBER,
                STATUS_ID,
                LAST_LOGIN
        );
        response.setUserName(USER_NAME);

        // Then
        assertThat(response.getUserName()).isEqualTo(USER_NAME);
        assertThat(response.getPassword()).isEqualTo(PASSWORD);
        assertThat(response.getMobileNumber()).isEqualTo(MOBILE_NUMBER);
        assertThat(response.getStatusId()).isEqualTo("1");
        assertThat(response.getLastLogin()).isEqualTo(LAST_LOGIN);
    }

    @Test
    void externalUserResponse_shouldTestBuilderAndToString() {
        // Given & When
        ExternalUserResponse response = ExternalUserResponse.builder()
                .userName(USER_NAME)
                .password(PASSWORD)
                .mobileNumber(MOBILE_NUMBER)
                .statusId(STATUS_ID)
                .lastLogin(LAST_LOGIN)
                .build();

        String toString = response.toString();

        // Then
        assertThat(response).isNotNull();
        assertThat(toString).isNotNull();
        assertThat(toString).contains(USER_NAME);
    }

    @Test
    void externalUserResponse_shouldTestNoArgsConstructor() {
        // When
        ExternalUserResponse response = new ExternalUserResponse();
        response.setUserName(USER_NAME);
        response.setPassword(PASSWORD);
        response.setMobileNumber(MOBILE_NUMBER);

        // Then
        assertThat(response.getUserName()).isEqualTo(USER_NAME);
        assertThat(response.getPassword()).isEqualTo(PASSWORD);
        assertThat(response.getMobileNumber()).isEqualTo(MOBILE_NUMBER);
    }
}