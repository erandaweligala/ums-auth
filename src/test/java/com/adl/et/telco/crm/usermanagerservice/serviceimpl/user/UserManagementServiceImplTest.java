package com.adl.et.telco.crm.usermanagerservice.serviceimpl.user;

import com.adl.et.telco.crm.usermanagerservice.client.AzureClient;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.AppRoleAssignmentsResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.TokenResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.user.*;
import com.adl.et.telco.crm.usermanagerservice.filter.FilterValueExtractor;
import com.adl.et.telco.crm.usermanagerservice.mapper.user.UserManagementMapper;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.*;
import com.adl.et.telco.crm.usermanagerservice.repository.*;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication.KeyCloakManagerService;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.azure.AzureAccessTokenService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UserManagementServiceImplTest {

    @Mock private CrmUserRepository crmUserRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserTypeRepository userTypeRepository;
    @Mock private DefaultGroupRepository defaultGroupRepository;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private ResponseHandler responseHandler;
    @Mock private UserManagementMapper userManagementMapper;
    @Mock private AzureAccessTokenService azureAccessTokenService;
    @Mock private AzureClient azureClient;
    @Mock private FilterValueExtractor filterValueExtractor;
    @Mock private KeyCloakManagerService keyCloakManagerService;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private static final String EMAIL = "test@example.com";
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        MDC.put(Constants.TENANT_ID, TENANT_ID.toString());
        // Set @Value fields
        ReflectionTestUtils.setField(userManagementService, "adRoleAssignmentRequired", true);
        ReflectionTestUtils.setField(userManagementService, "isKeyCloakEnabled", true);
        ReflectionTestUtils.setField(userManagementService, "tenantId", "test-tenant");
        ReflectionTestUtils.setField(userManagementService, "clientId", "test-client");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void createUser_shouldSucceed_whenDataIsValid() throws BaseException {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(EMAIL);
        request.setRoleId("1");
        request.setStatus("3"); // Ensure this matches your logic for triggering Azure
        request.setCreatedBy("admin");

        // Sync Mocks (Always used)
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(new Roles()));
        when(statusRepository.findById(anyLong())).thenReturn(Optional.of(new Status()));
        when(crmUserRepository.existsByUserName(anyString())).thenReturn(false);
        when(crmUserRepository.getUserForAuthByUserName(anyString())).thenReturn(Optional.of(new Users()));
        doReturn(new CommonAdaptorResp()).when(responseHandler).responseBuilder(anyString(), anyString());

        // Async Mocks (Wrap in lenient() to prevent UnnecessaryStubbingException)
        TokenResponse token = new TokenResponse();
        token.setAccessToken("token");
        lenient().when(azureAccessTokenService.generateAccessToken()).thenReturn(token);

        com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails azDetails =
                new com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails();
        azDetails.setId("uuid");
        CommonAdaptorResp azUserResp = new CommonAdaptorResp();
        azUserResp.setResponseData(azDetails);
        lenient().when(azureClient.getAzureUserDetails(anyString(), anyString())).thenReturn(azUserResp);

        AppRoleAssignmentsResponse appRoleResp = new AppRoleAssignmentsResponse();
        appRoleResp.setId("assigned-id");
        CommonAdaptorResp azGrantResp = new CommonAdaptorResp();
        azGrantResp.setResponseData(appRoleResp);
        lenient().when(azureClient.grantSSOAccessForADUser(anyString(), anyString())).thenReturn(azGrantResp);

        // When
        CommonAdaptorResp<String> response = userManagementService.createUser(request);

        // Then
        assertThat(response).isNotNull();
        verify(crmUserRepository, times(1)).save(any(Users.class));
    }

    @Test
    void validateEmail_Azure_Success() throws BaseException {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "Azure");

        when(azureAccessTokenService.getAccessToken()).thenReturn("token");
        CommonAdaptorResp azResp = new CommonAdaptorResp();
        azResp.setResponseData(new com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails());

        when(azureClient.getAzureUserDetails(anyString(), anyString())).thenReturn(azResp);
        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(0);
        doReturn(new CommonAdaptorResp()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<EmailValidateResponse> response = userManagementService.validateEmail(request);
        assertThat(response).isNotNull();
    }

    @Test
    void validateEmail_KeyCloak_Success() throws BaseException {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "KeyCloak");

        Map<String, Object> kcMap = new HashMap<>();
        kcMap.put("firstName", "John");
        kcMap.put("lastName", "Doe");
        when(keyCloakManagerService.getKeyCloakUserByEmail(anyString(), anyString(), anyString())).thenReturn(kcMap);
        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(0);

        doReturn(new CommonAdaptorResp()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        userManagementService.validateEmail(request);
        verify(keyCloakManagerService).getKeyCloakUserByEmail(anyString(), anyString(), anyString());
    }

    @Test
    void editUser_shouldSucceed() throws BaseException {
        EditUserRequest request = new EditUserRequest();
        request.setUserId("1");
        request.setRoleId("1");
        request.setStatus("1");

        when(crmUserRepository.findById(anyLong())).thenReturn(Optional.of(new Users()));
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(new Roles()));
        when(statusRepository.findById(anyLong())).thenReturn(Optional.of(new Status()));
        doReturn(new CommonAdaptorResp()).when(responseHandler).responseBuilder(anyString(), anyString());

        userManagementService.editUser(request);
        verify(crmUserRepository).save(any(Users.class));
    }

    @Test
    void getAllFilteredUserList_shouldSucceed() throws BaseException {
        // Given
        TableFilterRequest request = new TableFilterRequest();
        request.setFilterValues(new ArrayList<>());
        request.setLimit(10);
        request.setOffset(0);

        // 1. Mock the filter extractor to return an empty map (prevents internal NPEs)
        when(filterValueExtractor.extractFilterValues(any())).thenReturn(new HashMap<>());

        // 2. Create the mock data and explicitly set the Total Count
        // This prevents: "Cannot invoke java.lang.Integer.intValue() because getTotalCount() is null"
        AllUserDetails userDetails = new AllUserDetails();
        userDetails.setTotalCount(1);
        userDetails.setUserId("user123");
        userDetails.setName("Test User");

        // 3. Mock the repository call with 26 argument matchers
        when(crmUserRepository.getAllFilteredUser(
                any(), any(), any(), any(), any(), any(), // 1-6
                anyList(),                                // 7 (Filter values)
                any(), any(), any(), any(), any(), any(), // 8-13
                any(), any(), any(), any(), any(), any(), // 14-19
                any(), any(), any(), any(),                // 20-23
                anyInt(),                                 // 24 (Limit)
                anyInt(),                                 // 25 (Offset)
                anyLong()                                 // 26 (TenantId)
        )).thenReturn(List.of(userDetails));

        // 4. Mock the ResponseHandler to return a valid response object
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        doReturn(expectedResponse).when(responseHandler)
                .responseBuilder(anyString(), anyString(), anyList(), any());

        // When
        CommonAdaptorResp actualResponse = userManagementService.getAllFilteredUserList(request);

        // Then
        assertNotNull(actualResponse);
        verify(crmUserRepository).getAllFilteredUser(
                any(), any(), any(), any(), any(), any(),
                anyList(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(),
                anyInt(), anyInt(), anyLong()
        );
    }

    @Test
    void metadata_shouldCoverAllMethods() throws BaseException {
        doReturn(new CommonAdaptorResp()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        userManagementService.getUserStatusMetaData();
        userManagementService.getUserAccountMetaData();
        userManagementService.getUserTypeMetaData();
        userManagementService.getDefaultGroupMetaData();
        userManagementService.getUserGroupMetaData();

        verify(statusRepository).getUserStatus();
        verify(userAccountRepository).getUserAccountMetaData();
        verify(userTypeRepository).getUserTypeMetaData();
        verify(defaultGroupRepository).getDefaultGroupMetaData();
        verify(userGroupRepository).getUserGroupMetaData();
    }

    @Test
    void exception_shouldBeWrappedInBaseException() {
        when(crmUserRepository.findById(anyLong())).thenThrow(new RuntimeException("DB Error"));
        assertThatThrownBy(() -> userManagementService.getUserDetails(1L))
                .isInstanceOf(BaseException.class);
    }

    // ---- createUser additional coverage ----

    @Test
    void createUser_shouldThrowConflict_whenUserAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(EMAIL);
        request.setRoleId("1");
        request.setStatus("3");

        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(1);

        assertThatThrownBy(() -> userManagementService.createUser(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createUser_shouldThrowConflict_whenUsernameAlreadyTakenAfterExtraction() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(EMAIL);
        request.setRoleId("1");
        request.setStatus("3");

        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(0);
        // First call from getValidUserName returns false (name available), second call at line 97 returns true
        when(crmUserRepository.existsByUserName(anyString())).thenReturn(false, true);

        assertThatThrownBy(() -> userManagementService.createUser(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createUser_shouldSucceed_withRecursiveUsernameGeneration() throws BaseException {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(EMAIL);
        request.setRoleId("1");
        request.setStatus("3");
        request.setCreatedBy("admin");

        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(0);
        // Sequence: first call in getValidUserName=true (recurse), second=false (returns name), third at line 97=false
        when(crmUserRepository.existsByUserName(anyString())).thenReturn(true, false, false);
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(new Roles()));
        when(statusRepository.findById(anyLong())).thenReturn(Optional.of(new Status()));
        when(crmUserRepository.getUserForAuthByUserName(anyString())).thenReturn(Optional.empty());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString());

        CommonAdaptorResp<String> response = userManagementService.createUser(request);
        assertThat(response).isNotNull();
        verify(crmUserRepository).save(any(Users.class));
    }

    @Test
    void createUser_shouldSucceed_withoutAzureRoleAssignment() throws BaseException {
        ReflectionTestUtils.setField(userManagementService, "adRoleAssignmentRequired", false);

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(EMAIL);
        request.setRoleId("1");
        request.setStatus("3");
        request.setCreatedBy("admin");

        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(0);
        when(crmUserRepository.existsByUserName(anyString())).thenReturn(false);
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(new Roles()));
        when(statusRepository.findById(anyLong())).thenReturn(Optional.of(new Status()));
        when(crmUserRepository.getUserForAuthByUserName(anyString())).thenReturn(Optional.empty());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString());

        CommonAdaptorResp<String> response = userManagementService.createUser(request);
        assertThat(response).isNotNull();
        verifyNoInteractions(azureAccessTokenService);
    }

    @Test
    void createUser_shouldReturnSuccess_butNotSave_whenRolesEmpty() throws BaseException {
        ReflectionTestUtils.setField(userManagementService, "adRoleAssignmentRequired", false);

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(EMAIL);
        request.setRoleId("1");
        request.setStatus("3");

        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(0);
        when(crmUserRepository.existsByUserName(anyString())).thenReturn(false);
        when(roleRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(statusRepository.findById(anyLong())).thenReturn(Optional.of(new Status()));
        when(crmUserRepository.getUserForAuthByUserName(any())).thenReturn(Optional.empty());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString());

        CommonAdaptorResp<String> response = userManagementService.createUser(request);
        assertThat(response).isNotNull();
        verify(crmUserRepository, never()).save(any(Users.class));
    }

    // ---- editUser additional coverage ----

    @Test
    void editUser_shouldThrow_whenUserNotFound() {
        EditUserRequest request = new EditUserRequest();
        request.setUserId("1");
        request.setRoleId("1");
        request.setStatus("1");

        when(crmUserRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(new Roles()));
        when(statusRepository.findById(anyLong())).thenReturn(Optional.of(new Status()));
        when(crmUserRepository.getUserForAuthByUserName(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.editUser(request))
                .isInstanceOf(BaseException.class);
    }

    // ---- validateEmail additional coverage ----

    @Test
    void validateEmail_shouldThrowBadRequest_whenRequestIsNull() {
        assertThatThrownBy(() -> userManagementService.validateEmail(null))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void validateEmail_shouldThrowBadRequest_whenEmailIsEmpty() {
        EmailValidationRequest request = new EmailValidationRequest("", "Azure");
        assertThatThrownBy(() -> userManagementService.validateEmail(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void validateEmail_shouldThrowBadRequest_whenInvalidUserType() {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "SomeOtherType");
        assertThatThrownBy(() -> userManagementService.validateEmail(request))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void validateEmail_Azure_userNotFoundIn404_shouldReturnEmailNotFound() throws BaseException {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "Azure");
        BaseException notFoundEx = new BaseException("not found", "reason", HttpStatus.NOT_FOUND, "30", null);
        when(azureAccessTokenService.getAccessToken()).thenReturn("token");
        when(azureClient.getAzureUserDetails(anyString(), anyString())).thenAnswer(inv -> { throw notFoundEx; });
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<EmailValidateResponse> response = userManagementService.validateEmail(request);
        assertThat(response).isNotNull();
    }

    @Test
    void validateEmail_Azure_serverError_shouldReturnEmailNotFound() throws BaseException {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "Azure");
        BaseException serverEx = new BaseException("server error", "reason", HttpStatus.INTERNAL_SERVER_ERROR, "99", null);
        when(azureAccessTokenService.getAccessToken()).thenReturn("token");
        when(azureClient.getAzureUserDetails(anyString(), anyString())).thenAnswer(inv -> { throw serverEx; });
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<EmailValidateResponse> response = userManagementService.validateEmail(request);
        assertThat(response).isNotNull();
    }

    @Test
    void validateEmail_Azure_shouldReturnUserAlreadyExists_whenUserInDb() throws BaseException {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "Azure");

        com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails azDetails =
                new com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails();
        azDetails.setDisplayName("Test User");
        CommonAdaptorResp<com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails> azResp = new CommonAdaptorResp<>();
        azResp.setResponseData(azDetails);

        when(azureAccessTokenService.getAccessToken()).thenReturn("token");
        when(azureClient.getAzureUserDetails(anyString(), anyString())).thenReturn(azResp);
        when(crmUserRepository.isExistingUser(anyString(), anyList(), anyLong())).thenReturn(1);
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<EmailValidateResponse> response = userManagementService.validateEmail(request);
        assertThat(response).isNotNull();
    }

    @Test
    void validateEmail_KeyCloak_disabled_shouldReturnEmailNotFound() throws BaseException {
        ReflectionTestUtils.setField(userManagementService, "isKeyCloakEnabled", false);
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "KeyCloak");
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<EmailValidateResponse> response = userManagementService.validateEmail(request);
        assertThat(response).isNotNull();
        verifyNoInteractions(keyCloakManagerService);
    }

    @Test
    void validateEmail_KeyCloak_exceptionInFetch_shouldReturnEmailNotFound() throws BaseException {
        EmailValidationRequest request = new EmailValidationRequest(EMAIL, "KeyCloak");
        when(keyCloakManagerService.getKeyCloakUserByEmail(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<EmailValidateResponse> response = userManagementService.validateEmail(request);
        assertThat(response).isNotNull();
    }

    // ---- getUserDetails additional coverage ----

    @Test
    void getUserDetails_shouldSucceed_whenUserFound() throws BaseException {
        Users user = Users.builder().id(1L).status(new Status()).roles(Collections.emptyList()).build();
        when(crmUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userManagementMapper.mapUserDetails(any(Users.class))).thenReturn(new com.adl.et.telco.crm.usermanagerservice.dto.user.UserDetails());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<com.adl.et.telco.crm.usermanagerservice.dto.user.UserDetails> response =
                userManagementService.getUserDetails(1L);
        assertThat(response).isNotNull();
    }

    @Test
    void getUserDetails_shouldThrowNotFound_whenUserNotPresent() {
        when(crmUserRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.getUserDetails(1L))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- getUserDetailsByEmail additional coverage ----

    @Test
    void getUserDetailsByEmail_shouldReturnResponse_whenUserNotFound() throws BaseException {
        when(crmUserRepository.findByEmailAndTenantId(anyString(), anyLong())).thenReturn(Optional.empty());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), any());

        CommonAdaptorResp<com.adl.et.telco.crm.usermanagerservice.dto.user.UserDetails> response =
                userManagementService.getUserDetailsByEmail(EMAIL);
        assertThat(response).isNotNull();
    }

    // ---- getAllUsers additional coverage ----

    @Test
    void getAllUsers_shouldSucceed_withNonEmptyList() throws BaseException {
        AllUserDetails detail = new AllUserDetails();
        detail.setTotalCount(5);
        when(crmUserRepository.getAllUsers(anyInt(), anyInt(), any(), any(), any(), anyLong()))
                .thenReturn(List.of(detail));
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), anyList(), any());

        CommonAdaptorResp<List<AllUserDetails>> response = userManagementService.getAllUsers(10, 0, null, null, null);
        assertThat(response).isNotNull();
    }

    @Test
    void getAllUsers_shouldSucceed_withEmptyList() throws BaseException {
        when(crmUserRepository.getAllUsers(anyInt(), anyInt(), any(), any(), any(), anyLong()))
                .thenReturn(Collections.emptyList());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString(), anyList(), any());

        CommonAdaptorResp<List<AllUserDetails>> response = userManagementService.getAllUsers(10, 0, null, null, null);
        assertThat(response).isNotNull();
    }

    // ---- getAllFilteredUserList empty result branch ----

    @Test
    void getAllFilteredUserList_emptyResult() throws BaseException {
        TableFilterRequest request = new TableFilterRequest();
        request.setFilterValues(new ArrayList<>());
        request.setLimit(10);
        request.setOffset(0);

        when(filterValueExtractor.extractFilterValues(any())).thenReturn(new HashMap<>());
        when(crmUserRepository.getAllFilteredUser(
                any(), any(), any(), any(), any(), any(),
                anyList(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(),
                anyInt(), anyInt(), anyLong()
        )).thenReturn(Collections.emptyList());
        doReturn(new CommonAdaptorResp<>()).when(responseHandler)
                .responseBuilder(anyString(), anyString(), anyList(), any());

        CommonAdaptorResp<List<AllUserDetails>> response = userManagementService.getAllFilteredUserList(request);
        assertThat(response).isNotNull();
    }

    // ---- expire additional coverage ----

    @Test
    void expire_shouldSucceed() throws BaseException {
        doReturn(new CommonAdaptorResp<>()).when(responseHandler).responseBuilder(anyString(), anyString());

        CommonAdaptorResp<String> response = userManagementService.expire(30);
        assertThat(response).isNotNull();
        verify(crmUserRepository).expireUser(eq(30), anyLong());
    }
}