package com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication;

import com.adl.et.telco.crm.usermanagerservice.client.KeyCloakClient;
import com.adl.et.telco.crm.usermanagerservice.dto.authentication.*;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.dto.user.UserAuthDTO;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.*;
import com.adl.et.telco.crm.usermanagerservice.repository.AttributeRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.KeycloakRepository;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceTest {

    @Mock
    private CrmUserRepository crmUserRepository;

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private KeycloakRepository keycloakRepository;

    @Mock
    private KeyCloakClient keyCloakClient;

    @InjectMocks
    private UserDetailsService userDetailsService;

    private static final String USER_NAME = "john.doe";
    private static final String TENANT_ID = "1";
    private static final Long TENANT_ID_LONG = 1L;
    private static final String CLIENT_ID = "client123";
    private static final String CLIENT_SECRET = "secret456";

    private Users testUser;
    private Roles testRole;
    private Tenants testTenant;
    private Status testStatus;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        MDC.put(Constants.TENANT_ID, TENANT_ID);

        // Setup test data
        testTenant = new Tenants();
        testTenant.setId(TENANT_ID_LONG);

        testStatus = new Status();
        testStatus.setId(1L);
        testStatus.setName("Active");

        testRole = new Roles();
        testRole.setId(1L);
        testRole.setName("Admin");
        testRole.setTenants(testTenant);

        testUser = Users.builder()
                .id(1L)
                .userName(USER_NAME)
                .name("John Doe")
                .email("john.doe@example.com")
                .mobileNumber("+1234567890")
                .status(testStatus)
                .sessionId("old-session-id")
                .lastLoginDatetime("2024-01-01 10:00:00")
                .roles(List.of(testRole))
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void getBasicInfoByUserName_shouldReturnUserBasicInfo_whenUserExistsWithPermissions() throws BaseException {
        // Given
        Menus menu = new Menus();
        menu.setId(1L);

        Components component = new Components();
        component.setId(1L);

        Actions action = new Actions();
        action.setId(1L);
        action.setTenantsList(List.of(testTenant));

        Attributes attribute = new Attributes();
        attribute.setId(1L);
        attribute.setTenantsList(List.of(testTenant));
        attribute.setActions(action);

        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setMenus(menu);
        testPermission.setComponents(component);
        testPermission.setActionsList(List.of(action));
        testPermission.setAttributesList(List.of(attribute));

        testRole.setPermissionList(List.of(testPermission));

        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenReturn(1);
        when(crmUserRepository.getUserForAuthByUserName(USER_NAME))
                .thenReturn(Optional.of(testUser));
        when(attributeRepository.getControllableAttributes(anyList(), eq(TENANT_ID_LONG)))
                .thenReturn(List.of(attribute));

        // When
        CommonAdaptorResp<UserBasicInfo> response = userDetailsService.getBasicInfoByUserName(USER_NAME);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getUserName()).isEqualTo(USER_NAME);
        assertThat(response.getResponseData().getName()).isEqualTo("John Doe");
        assertThat(response.getResponseData().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getResponseData().getMsisdn()).isEqualTo("+1234567890");
        assertThat(response.getResponseData().getRole()).isEqualTo("Admin");
        assertThat(response.getResponseData().getStatus()).isEqualTo("Active");
        assertThat(response.getResponseData().getPermission()).isNotNull();
        assertThat(response.getResponseData().getPermission().getMenuids()).hasSize(1);
        assertThat(response.getResponseData().getPermission().getComponents()).hasSize(1);

        verify(crmUserRepository, times(1)).updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME));
        verify(crmUserRepository, times(1)).getUserForAuthByUserName(USER_NAME);
        verify(attributeRepository, times(1)).getControllableAttributes(anyList(), eq(TENANT_ID_LONG));
    }

    @Test
    void getBasicInfoByUserName_shouldReturnUserWithNullRole_whenNoRoleMatchesTenant() throws BaseException {
        // Given
        Tenants differentTenant = new Tenants();
        differentTenant.setId(999L);

        Roles differentRole = new Roles();
        differentRole.setId(2L);
        differentRole.setName("User");
        differentRole.setTenants(differentTenant);
        differentRole.setPermissionList(new ArrayList<>());

        testUser.setRoles(List.of(differentRole));

        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenReturn(1);
        when(crmUserRepository.getUserForAuthByUserName(USER_NAME))
                .thenReturn(Optional.of(testUser));

        // When
        CommonAdaptorResp<UserBasicInfo> response = userDetailsService.getBasicInfoByUserName(USER_NAME);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getRole()).isNull();
        assertThat(response.getResponseData().getPermission()).isNull();

        verify(crmUserRepository, times(1)).updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME));
        verify(crmUserRepository, times(1)).getUserForAuthByUserName(USER_NAME);
    }

    @Test
    void getBasicInfoByUserName_shouldThrowBaseException_whenUpdateLastLoginFails() {
        // Given
        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getBasicInfoByUserName(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.EXCEPTION_REPOSITORY_LAYER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_REPOSITORY_LAYER.code());
                });

        verify(crmUserRepository, times(1)).updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME));
        verify(crmUserRepository, never()).getUserForAuthByUserName(anyString());
    }

    @Test
    void getBasicInfoByUserName_shouldThrowBaseException_whenUserNotFound() {
        // Given
        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenReturn(1);
        when(crmUserRepository.getUserForAuthByUserName(USER_NAME))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getBasicInfoByUserName(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage(ResponseCodeEnum.INVALID_USER.description())
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.INVALID_USER.code());
                });

        verify(crmUserRepository, times(1)).updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME));
        verify(crmUserRepository, times(1)).getUserForAuthByUserName(USER_NAME);
    }

    @Test
    void getBasicInfoByUserName_shouldRethrowBaseException_whenBaseExceptionOccurs() {
        // Given
        BaseException originalException = new BaseException(
                "Custom error",
                "Custom reason",
                HttpStatus.UNAUTHORIZED,
                "401",
                null);

        doAnswer(invocation -> {
            throw originalException;
        }).when(crmUserRepository).updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getBasicInfoByUserName(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage("Custom error")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(baseException.getResultCode()).isEqualTo("401");
                });
    }

    @Test
    void getBasicInfoByUserName_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getBasicInfoByUserName(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                });
    }

    @Test
    void getAttributeDetails_shouldReturnAttributeList_whenAttributesExist() throws BaseException {
        // Given
        Actions action1 = new Actions();
        action1.setId(1L);

        Actions action2 = new Actions();
        action2.setId(2L);

        Attributes attr1 = new Attributes();
        attr1.setId(1L);
        attr1.setPath("/path1");
        attr1.setActions(action1);

        Attributes attr2 = new Attributes();
        attr2.setId(2L);
        attr2.setPath("/path2");
        attr2.setActions(action1);

        Attributes attr3 = new Attributes();
        attr3.setId(3L);
        attr3.setPath("/path3");
        attr3.setActions(action2);

        List<Attributes> attributesList = Arrays.asList(attr1, attr2, attr3);

        when(attributeRepository.getAttributesDetails(TENANT_ID_LONG))
                .thenReturn(attributesList);

        // When
        CommonAdaptorResp<List<AttributeDetails>> response = userDetailsService.getAttributeDetails();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).hasSize(2);
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());

        AttributeDetails firstAction = response.getResponseData().stream()
                .filter(ad -> ad.getActionId().equals(1L))
                .findFirst()
                .orElse(null);
        assertThat(firstAction).isNotNull();
        assertThat(firstAction.getAttributeList()).hasSize(2);

        AttributeDetails secondAction = response.getResponseData().stream()
                .filter(ad -> ad.getActionId().equals(2L))
                .findFirst()
                .orElse(null);
        assertThat(secondAction).isNotNull();
        assertThat(secondAction.getAttributeList()).hasSize(1);

        verify(attributeRepository, times(1)).getAttributesDetails(TENANT_ID_LONG);
    }

    @Test
    void getAttributeDetails_shouldReturnEmptyList_whenNoAttributesExist() throws BaseException {
        // Given
        when(attributeRepository.getAttributesDetails(TENANT_ID_LONG))
                .thenReturn(new ArrayList<>());

        // When
        CommonAdaptorResp<List<AttributeDetails>> response = userDetailsService.getAttributeDetails();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEmpty();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());

        verify(attributeRepository, times(1)).getAttributesDetails(TENANT_ID_LONG);
    }

    @Test
    void getAttributeDetails_shouldReturnEmptyList_whenAttributesListIsNull() throws BaseException {
        // Given
        when(attributeRepository.getAttributesDetails(TENANT_ID_LONG))
                .thenReturn(null);

        // When
        CommonAdaptorResp<List<AttributeDetails>> response = userDetailsService.getAttributeDetails();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEmpty();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());

        verify(attributeRepository, times(1)).getAttributesDetails(TENANT_ID_LONG);
    }

    @Test
    void getAttributeDetails_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        when(attributeRepository.getAttributesDetails(TENANT_ID_LONG))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getAttributeDetails())
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                });

        verify(attributeRepository, times(1)).getAttributesDetails(TENANT_ID_LONG);
    }

    @Test
    void getUsername_shouldReturnUsername_whenClientSecretExists() throws BaseException {
        // Given
        UserAuthDTO userAuthDTO = UserAuthDTO.builder()
                .code("auth_code_123")
                .tenant(TENANT_ID)
                .clientId(CLIENT_ID)
                .redirectUri("http://localhost/callback")
                .build();

        CommonAdaptorResp<String> keycloakResponse = CommonAdaptorResp.<String>builder()
                .result(Result.builder()
                        .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                        .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                        .build())
                .responseData("john.doe")
                .build();

        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getKeyCloakUserDetails(CLIENT_SECRET, userAuthDTO))
                .thenReturn(keycloakResponse);

        // When
        CommonAdaptorResp<String> response = userDetailsService.getUsername(userAuthDTO);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEqualTo("john.doe");
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserDetails(CLIENT_SECRET, userAuthDTO);
    }

    @Test
    void getUsername_shouldReturnEmptyUsername_whenClientSecretNotFound() throws BaseException {
        // Given
        UserAuthDTO userAuthDTO = UserAuthDTO.builder()
                .code("auth_code_123")
                .tenant(TENANT_ID)
                .clientId(CLIENT_ID)
                .redirectUri("http://localhost/callback")
                .build();

        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.empty());

        // When
        CommonAdaptorResp<String> response = userDetailsService.getUsername(userAuthDTO);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEmpty();
        assertThat(response.getResult().getResultCode()).isEqualTo(ResponseCodeEnum.SUCCESSFUL.code());

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, never()).getKeyCloakUserDetails(anyString(), any());
    }

    @Test
    void getUsername_shouldThrowBaseException_whenKeycloakClientThrowsException() throws BaseException {
        // Given
        UserAuthDTO userAuthDTO = UserAuthDTO.builder()
                .code("auth_code_123")
                .tenant(TENANT_ID)
                .clientId(CLIENT_ID)
                .redirectUri("http://localhost/callback")
                .build();

        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenReturn(Optional.of(CLIENT_SECRET));
        when(keyCloakClient.getKeyCloakUserDetails(CLIENT_SECRET, userAuthDTO))
                .thenThrow(new RuntimeException("Keycloak connection failed"));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getUsername(userAuthDTO))
                .isInstanceOf(BaseException.class)
                .hasMessage("Keycloak connection failed")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
        verify(keyCloakClient, times(1)).getKeyCloakUserDetails(CLIENT_SECRET, userAuthDTO);
    }

    @Test
    void getUsername_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        UserAuthDTO userAuthDTO = UserAuthDTO.builder()
                .code("auth_code_123")
                .tenant(TENANT_ID)
                .clientId(CLIENT_ID)
                .redirectUri("http://localhost/callback")
                .build();

        when(keycloakRepository.findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getUsername(userAuthDTO))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error")
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code());
                });

        verify(keycloakRepository, times(1)).findClientSecretByAppAndTenant(TENANT_ID, CLIENT_ID);
    }

    @Test
    void getUserPermission_shouldThrowBaseException_whenAttributeRepositoryFails() {
        // Given
        Menus menu = new Menus();
        menu.setId(1L);

        Components component = new Components();
        component.setId(1L);

        Actions action = new Actions();
        action.setId(1L);
        action.setTenantsList(List.of(testTenant));

        Attributes attribute = new Attributes();
        attribute.setId(1L);
        attribute.setTenantsList(List.of(testTenant));

        testPermission = new Permission();
        testPermission.setMenus(menu);
        testPermission.setComponents(component);
        testPermission.setActionsList(List.of(action));
        testPermission.setAttributesList(List.of(attribute));

        testRole.setPermissionList(List.of(testPermission));

        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenReturn(1);
        when(crmUserRepository.getUserForAuthByUserName(USER_NAME))
                .thenReturn(Optional.of(testUser));
        when(attributeRepository.getControllableAttributes(anyList(), eq(TENANT_ID_LONG)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.getBasicInfoByUserName(USER_NAME))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error");
    }

    @Test
    void getBasicInfoByUserName_shouldHandleMultiplePermissions() throws BaseException {
        // Given
        Menus menu1 = new Menus();
        menu1.setId(1L);
        Menus menu2 = new Menus();
        menu2.setId(2L);

        Components component1 = new Components();
        component1.setId(1L);
        Components component2 = new Components();
        component2.setId(2L);

        Actions action = new Actions();
        action.setId(1L);
        action.setTenantsList(List.of(testTenant));

        Attributes attribute = new Attributes();
        attribute.setId(1L);
        attribute.setTenantsList(List.of(testTenant));

        Permission permission1 = new Permission();
        permission1.setMenus(menu1);
        permission1.setComponents(component1);
        permission1.setActionsList(List.of(action));
        permission1.setAttributesList(List.of(attribute));

        Permission permission2 = new Permission();
        permission2.setMenus(menu2);
        permission2.setComponents(component2);
        permission2.setActionsList(List.of(action));
        permission2.setAttributesList(List.of(attribute));

        testRole.setPermissionList(Arrays.asList(permission1, permission2));

        when(crmUserRepository.updateCrmUserLastLogTimeAndTid(anyString(), anyString(), eq(USER_NAME)))
                .thenReturn(1);
        when(crmUserRepository.getUserForAuthByUserName(USER_NAME))
                .thenReturn(Optional.of(testUser));
        when(attributeRepository.getControllableAttributes(anyList(), eq(TENANT_ID_LONG)))
                .thenReturn(List.of(attribute));

        // When
        CommonAdaptorResp<UserBasicInfo> response = userDetailsService.getBasicInfoByUserName(USER_NAME);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData().getPermission()).isNotNull();
        assertThat(response.getResponseData().getPermission().getMenuids()).hasSize(2);
        assertThat(response.getResponseData().getPermission().getComponents()).hasSize(2);
    }
}