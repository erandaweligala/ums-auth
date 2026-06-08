package com.adl.et.telco.crm.usermanagerservice.serviceimpl.audit;

import com.adl.et.telco.crm.usermanagerservice.dto.audit.ActionDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Actions;
import com.adl.et.telco.crm.usermanagerservice.repository.ActionsRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditSupportServiceImplTest {

    @Mock
    private ActionsRepository actionsRepository;

    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private CrmUserRepository crmUserRepository;

    @InjectMocks
    private AuditSupportServiceImpl auditSupportService;

    private static final Long TENANT_ID = 1L;
    private static final Long ACTION_ID = 100L;

    @BeforeEach
    void setUp() {
        MDC.put(Constants.TENANT_ID, String.valueOf(TENANT_ID));
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void getActionDetails_shouldReturnActionDetails_whenActionExists() throws BaseException {
        // Given
        Actions action = new Actions();
        action.setId(ACTION_ID);
        action.setName("CREATE_USER");
        action.setDescription("Create a new user");

        ActionDetails expectedActionDetails = ActionDetails.builder()
                .id(ACTION_ID)
                .name("CREATE_USER")
                .description("Create a new user")
                .build();

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<ActionDetails> expectedResponse = CommonAdaptorResp.<ActionDetails>builder()
                .result(result)
                .responseData(expectedActionDetails)
                .build();

        when(actionsRepository.findById(ACTION_ID)).thenReturn(Optional.of(action));
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(ActionDetails.class)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<ActionDetails> response = auditSupportService.getActionDetails(ACTION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());
        assertThat(response.getResult().getResultDescription()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.description());
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getId()).isEqualTo(ACTION_ID);
        assertThat(response.getResponseData().getName()).isEqualTo("CREATE_USER");
        assertThat(response.getResponseData().getDescription()).isEqualTo("Create a new user");

        verify(actionsRepository, times(1)).findById(ACTION_ID);
        verify(responseHandler, times(1)).responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(ActionDetails.class));
    }

    @Test
    void getActionDetails_shouldThrowBaseException_whenActionNotFound() {
        // Given
        when(actionsRepository.findById(ACTION_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getActionDetails(ACTION_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.INVALID_ACTION.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.INVALID_ACTION.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.INVALID_ACTION.description());
                    assertThat(baseException.getStackTraceElements()).isNull();
                });

        verify(actionsRepository, times(1)).findById(ACTION_ID);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getActionDetails_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database connection failed");
        when(actionsRepository.findById(ACTION_ID)).thenThrow(repositoryException);

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getActionDetails(ACTION_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database connection failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(actionsRepository, times(1)).findById(ACTION_ID);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getActionDetails_shouldRethrowBaseException_whenBaseExceptionOccurs() {
        // Given
        BaseException baseException = new BaseException(
                "Custom error",
                "Custom description",
                HttpStatus.FORBIDDEN,
                "403",
                null);

        // Use doAnswer to throw the checked exception
        doAnswer(invocation -> {
            throw baseException;
        }).when(actionsRepository).findById(ACTION_ID);

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getActionDetails(ACTION_ID))
                .isInstanceOf(BaseException.class)
                .hasMessage("Custom error")
                .satisfies(exception -> {
                    BaseException be = (BaseException) exception;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getResultCode()).isEqualTo("403");
                    assertThat(be.getReason()).isEqualTo("Custom description");
                    assertThat(be.getStackTraceElements()).isNull();
                });

        verify(actionsRepository, times(1)).findById(ACTION_ID);
    }

    @Test
    void getAllUsers_shouldReturnListOfUsers_whenUsersExist() throws BaseException {
        // Given
        List<String> usersList = Arrays.asList("user1@example.com", "user2@example.com", "user3@example.com");

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<List<String>> expectedResponse = CommonAdaptorResp.<List<String>>builder()
                .result(result)
                .responseData(usersList)
                .build();

        when(crmUserRepository.findByTenantId(TENANT_ID)).thenReturn(usersList);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(usersList)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<String>> response = auditSupportService.getAllUsers();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());
        assertThat(response.getResult().getResultDescription()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.description());
        assertThat(response.getResponseData()).hasSize(3);
        assertThat(response.getResponseData()).containsExactly("user1@example.com", "user2@example.com", "user3@example.com");

        verify(crmUserRepository, times(1)).findByTenantId(TENANT_ID);
        verify(responseHandler, times(1)).responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(usersList));
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenNoUsersExist() throws BaseException {
        // Given
        List<String> emptyUsersList = new ArrayList<>();

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<List<String>> expectedResponse = CommonAdaptorResp.<List<String>>builder()
                .result(result)
                .responseData(emptyUsersList)
                .build();

        when(crmUserRepository.findByTenantId(TENANT_ID)).thenReturn(emptyUsersList);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(emptyUsersList)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<String>> response = auditSupportService.getAllUsers();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEmpty();

        verify(crmUserRepository, times(1)).findByTenantId(TENANT_ID);
    }

    @Test
    void getAllUsers_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database error");
        when(crmUserRepository.findByTenantId(TENANT_ID)).thenThrow(repositoryException);

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getAllUsers())
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(crmUserRepository, times(1)).findByTenantId(TENANT_ID);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getAllUsers_shouldThrowBaseException_whenTenantIdIsInvalid() {
        // Given
        MDC.put(Constants.TENANT_ID, "invalid");

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getAllUsers())
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(crmUserRepository, never()).findByTenantId(anyLong());
    }

    @Test
    void getActions_shouldReturnListOfActions_whenActionsExist() throws BaseException {
        // Given
        List<ActionDetails> actionsList = Arrays.asList(
                ActionDetails.builder().id(1L).name("CREATE").description("Create action").build(),
                ActionDetails.builder().id(2L).name("UPDATE").description("Update action").build(),
                ActionDetails.builder().id(3L).name("DELETE").description("Delete action").build()
        );

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<List<ActionDetails>> expectedResponse = CommonAdaptorResp.<List<ActionDetails>>builder()
                .result(result)
                .responseData(actionsList)
                .build();

        when(actionsRepository.getActions(TENANT_ID)).thenReturn(actionsList);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(actionsList)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<ActionDetails>> response = auditSupportService.getActions();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());
        assertThat(response.getResult().getResultDescription()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.description());
        assertThat(response.getResponseData()).hasSize(3);
        assertThat(response.getResponseData().get(0).getName()).isEqualTo("CREATE");
        assertThat(response.getResponseData().get(1).getName()).isEqualTo("UPDATE");
        assertThat(response.getResponseData().get(2).getName()).isEqualTo("DELETE");

        verify(actionsRepository, times(1)).getActions(TENANT_ID);
        verify(responseHandler, times(1)).responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(actionsList));
    }

    @Test
    void getActions_shouldReturnEmptyList_whenNoActionsExist() throws BaseException {
        // Given
        List<ActionDetails> emptyActionsList = new ArrayList<>();

        Result result = Result.builder()
                .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                .build();

        CommonAdaptorResp<List<ActionDetails>> expectedResponse = CommonAdaptorResp.<List<ActionDetails>>builder()
                .result(result)
                .responseData(emptyActionsList)
                .build();

        when(actionsRepository.getActions(TENANT_ID)).thenReturn(emptyActionsList);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(emptyActionsList)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<ActionDetails>> response = auditSupportService.getActions();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEmpty();

        verify(actionsRepository, times(1)).getActions(TENANT_ID);
    }

    @Test
    void getActions_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Query execution failed");
        when(actionsRepository.getActions(TENANT_ID)).thenThrow(repositoryException);

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getActions())
                .isInstanceOf(BaseException.class)
                .hasMessage("Query execution failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(actionsRepository, times(1)).getActions(TENANT_ID);
        verify(responseHandler, never()).responseBuilder(anyString(), anyString(), any());
    }

    @Test
    void getActions_shouldThrowBaseException_whenTenantIdIsInvalid() {
        // Given
        MDC.put(Constants.TENANT_ID, "not-a-number");

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getActions())
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(actionsRepository, never()).getActions(anyLong());
    }

    @Test
    void getActions_shouldThrowBaseException_whenTenantIdIsNull() {
        // Given
        MDC.remove(Constants.TENANT_ID);

        // When & Then
        assertThatThrownBy(() -> auditSupportService.getActions())
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                    assertThat(baseException.getReason()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description());
                    assertThat(baseException.getStackTraceElements()).isNotNull();
                });

        verify(actionsRepository, never()).getActions(anyLong());
    }
}