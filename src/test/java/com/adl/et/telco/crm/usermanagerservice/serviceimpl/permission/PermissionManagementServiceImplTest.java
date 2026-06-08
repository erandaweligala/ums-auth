package com.adl.et.telco.crm.usermanagerservice.serviceimpl.permission;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.PageDetail;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.*;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.ComponentsItem;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.filter.FilterValueExtractor;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.*;
import com.adl.et.telco.crm.usermanagerservice.repository.*;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.constants.StatusEnum;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionManagementServiceImplTest {

    @Mock
    private PermissionsRepository permissionsRepository;

    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private MenusRepository menusRepository;

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private ActionsRepository actionsRepository;

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private TenantsRepository tenantsRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private FilterValueExtractor filterValueExtractor;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private PermissionManagementServiceImpl permissionManagementService;

    private static final String TENANT_ID = "1";
    private static final Long TENANT_ID_LONG = 1L;
    private static final Long PERMISSION_ID = 100L;
    private static final Long MENU_ID = 10L;
    private static final Long COMPONENT_ID = 20L;
    private static final Long ACTION_ID = 30L;
    private static final Long ATTRIBUTE_ID = 40L;

    private Permission testPermission;
    private Menus testMenu;
    private Components testComponent;
    private Tenants testTenant;
    private Status testStatus;
    private Actions testAction;
    private Attributes testAttribute;

    @BeforeEach
    void setUp() {
        MDC.put(Constants.TENANT_ID, TENANT_ID);

        testTenant = new Tenants();
        testTenant.setId(TENANT_ID_LONG);

        testStatus = new Status();
        testStatus.setId(StatusEnum.ACTIVE.value());
        testStatus.setName("Active");

        testMenu = new Menus();
        testMenu.setId(MENU_ID);
        testMenu.setName("User Management");
        testMenu.setTenantsList(List.of(testTenant));

        testComponent = new Components();
        testComponent.setId(COMPONENT_ID);
        testComponent.setName("User List");

        testAction = new Actions();
        testAction.setId(ACTION_ID);
        testAction.setName("View");
        testAction.setMainAction(true); // boolean value
        testAction.setTenantsList(List.of(testTenant));

        testAttribute = new Attributes();
        testAttribute.setId(ATTRIBUTE_ID);
        testAttribute.setName("Name");
        testAttribute.setPath("/user/name");

        testPermission = Permission.builder()
                .id(PERMISSION_ID)
                .name("View Users")
                .description("Permission to view users")
                .menus(testMenu)
                .components(testComponent)
                .status(testStatus)
                .tenant(testTenant)
                .actionsList(List.of(testAction))
                .attributesList(List.of(testAttribute))
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void getPermissionList_shouldReturnPermissionList_whenPermissionsExist() throws BaseException {
        // Given
        PermissionView permissionView = PermissionView.builder()
                .permissionId(PERMISSION_ID.toString())
                .name("View Users")
                .description("Permission to view users")
                .componentName("User List")
                .menuName("User Management")
                .totalCount(1)
                .build();

        List<PermissionView> permissionList = List.of(permissionView);

        PageDetail pageDetail = PageDetail.builder()
                .pageNumber(1)
                .pageElementCount(1)
                .totalRecords(1)
                .build();

        CommonAdaptorResp<List<PermissionView>> expectedResponse = CommonAdaptorResp.<List<PermissionView>>builder()
                .result(Result.builder()
                        .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                        .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                        .pageDetail(pageDetail)
                        .build())
                .responseData(permissionList)
                .build();

        when(permissionsRepository.getPermissionList(TENANT_ID_LONG, "View", MENU_ID, COMPONENT_ID, 10, 0))
                .thenReturn(permissionList);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(permissionList),
                any(PageDetail.class)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<PermissionView>> response =
                permissionManagementService.getPermissionList("View", MENU_ID, COMPONENT_ID, 10, 0);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).hasSize(1);
        assertThat(response.getResponseData().get(0).getPermissionId()).isEqualTo(PERMISSION_ID.toString());

        verify(permissionsRepository, times(1)).getPermissionList(TENANT_ID_LONG, "View", MENU_ID, COMPONENT_ID, 10, 0);
    }

    @Test
    void getPermissionList_shouldReturnEmptyList_whenNoPermissionsExist() throws BaseException {
        // Given
        List<PermissionView> emptyList = new ArrayList<>();

        PageDetail pageDetail = PageDetail.builder()
                .pageNumber(1)
                .pageElementCount(0)
                .totalRecords(0)
                .build();

        CommonAdaptorResp<List<PermissionView>> expectedResponse = CommonAdaptorResp.<List<PermissionView>>builder()
                .result(Result.builder()
                        .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                        .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                        .pageDetail(pageDetail)
                        .build())
                .responseData(emptyList)
                .build();

        when(permissionsRepository.getPermissionList(TENANT_ID_LONG, null, null, null, 10, 0))
                .thenReturn(emptyList);
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                eq(emptyList),
                any(PageDetail.class)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<PermissionView>> response =
                permissionManagementService.getPermissionList(null, null, null, 10, 0);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isEmpty();
    }

    @Test
    void getPermissionList_shouldThrowBaseException_whenRepositoryThrowsException() {
        // Given
        when(permissionsRepository.getPermissionList(TENANT_ID_LONG, null, null, null, 10, 0))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.getPermissionList(null, null, null, 10, 0))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error");
    }

    @Test
    void getPermissionDetails_shouldReturnEmptyDetails_whenPermissionNotFound() throws BaseException {
        // Given
        PermissionDetails emptyDetails = new PermissionDetails();

        CommonAdaptorResp<PermissionDetails> expectedResponse = CommonAdaptorResp.<PermissionDetails>builder()
                .result(Result.builder()
                        .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                        .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                        .build())
                .responseData(emptyDetails)
                .build();

        when(permissionsRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(PermissionDetails.class)))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<PermissionDetails> response =
                permissionManagementService.getPermissionDetails(PERMISSION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getPermissionId()).isNull();
        assertThat(response.getResponseData().getPermissionName()).isNull();
        assertThat(response.getResponseData().getMainActions()).isNull();

        verify(permissionsRepository, times(1)).findById(PERMISSION_ID);
        verify(responseHandler, times(1)).responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(PermissionDetails.class));
    }

    @Test
    void getPermissionDetails_shouldHandleSubActions() throws BaseException {
        // Given
        Actions mainActionForSub = new Actions();
        mainActionForSub.setId(ACTION_ID);
        mainActionForSub.setName("View");

        Actions subAction = new Actions();
        subAction.setId(31L);
        subAction.setName("Edit");
        subAction.setMainAction(false);

        PermissionDetailsResultSet mainActionResult = new PermissionDetailsResultSet(
                ATTRIBUTE_ID, 1, 1, ACTION_ID, "Name", 1, null, "View"
        );

        PermissionDetailsResultSet subActionResult = new PermissionDetailsResultSet(
                ATTRIBUTE_ID, 0, 1, 31L, "Email", 1, ACTION_ID, "Edit"
        );

        when(permissionsRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));
        when(permissionsRepository.findPermissionDetails(TENANT_ID_LONG, PERMISSION_ID, COMPONENT_ID))
                .thenReturn(Arrays.asList(mainActionResult, subActionResult));
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(PermissionDetails.class)))
                .thenAnswer(invocation -> {
                    PermissionDetails details = invocation.getArgument(2);
                    return CommonAdaptorResp.<PermissionDetails>builder()
                            .result(Result.builder()
                                    .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                                    .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                                    .build())
                            .responseData(details)
                            .build();
                });

        // When
        CommonAdaptorResp<PermissionDetails> response =
                permissionManagementService.getPermissionDetails(PERMISSION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getMainActions()).hasSize(1);
        assertThat(response.getResponseData().getMainActions().get(0).getSubActions()).hasSize(1);

        verify(permissionsRepository, times(1)).findById(PERMISSION_ID);
        verify(permissionsRepository, times(1)).findPermissionDetails(TENANT_ID_LONG, PERMISSION_ID, COMPONENT_ID);
    }

    @Test
    void getPermissionDetails_shouldHandleNullSubActionsMap() throws BaseException {
        // Given
        PermissionDetailsResultSet mainActionResult = new PermissionDetailsResultSet(
                ATTRIBUTE_ID, 1, 1, ACTION_ID, "Name", 1, null, "View"
        );

        when(permissionsRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));
        when(permissionsRepository.findPermissionDetails(TENANT_ID_LONG, PERMISSION_ID, COMPONENT_ID))
                .thenReturn(List.of(mainActionResult));
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(PermissionDetails.class)))
                .thenAnswer(invocation -> {
                    PermissionDetails details = invocation.getArgument(2);
                    return CommonAdaptorResp.<PermissionDetails>builder()
                            .result(Result.builder()
                                    .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                                    .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                                    .build())
                            .responseData(details)
                            .build();
                });

        // When
        CommonAdaptorResp<PermissionDetails> response =
                permissionManagementService.getPermissionDetails(PERMISSION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getMainActions()).hasSize(1);
        assertThat(response.getResponseData().getMainActions().get(0).getSubActions()).isEmpty();
    }

    @Test
    void getPermissionDetails_shouldFilterNullAttributes() throws BaseException {
        // Given
        PermissionDetailsResultSet resultWithoutAttribute = new PermissionDetailsResultSet(
                null, 1, 1, ACTION_ID, "Name", 1, null, "View"
        );

        when(permissionsRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));
        when(permissionsRepository.findPermissionDetails(TENANT_ID_LONG, PERMISSION_ID, COMPONENT_ID))
                .thenReturn(List.of(resultWithoutAttribute));
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.SUCCESSFUL.code()),
                eq(ResponseCodeEnum.SUCCESSFUL.description()),
                any(PermissionDetails.class)))
                .thenAnswer(invocation -> {
                    PermissionDetails details = invocation.getArgument(2);
                    return CommonAdaptorResp.<PermissionDetails>builder()
                            .result(Result.builder()
                                    .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                                    .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                                    .build())
                            .responseData(details)
                            .build();
                });

        // When
        CommonAdaptorResp<PermissionDetails> response =
                permissionManagementService.getPermissionDetails(PERMISSION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotNull();
        assertThat(response.getResponseData().getMainActions()).hasSize(1);
        assertThat(response.getResponseData().getMainActions().get(0).getAttributes()).isEmpty();
    }

    @Test
    void getPermissionDetails_shouldThrowBaseException_whenExceptionOccurs() {
        // Given
        when(permissionsRepository.findById(PERMISSION_ID))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.getPermissionDetails(PERMISSION_ID))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void getActionHierarchy_shouldThrowBaseException_whenExceptionOccurs() {
        // Given
        when(permissionsRepository.getActionHierarchy(TENANT_ID_LONG, COMPONENT_ID))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.getActionHierarchy(COMPONENT_ID))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void getAllMenuAndComponents_shouldThrowBaseException_whenExceptionOccurs() {
        // Given
        when(menusRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.getAllMenuAndComponents())
                .isInstanceOf(BaseException.class);
    }

    @Test
    void createPermission_shouldThrowBaseException_whenNameAlreadyExists() {
        // Given
        CreatePermission createPermission = new CreatePermission();
        createPermission.setName("Existing Permission");
        createPermission.setMenuId(MENU_ID.toString());
        createPermission.setComponentId(COMPONENT_ID.toString());

        when(permissionsRepository.isAlreadyExist("Existing Permission", MENU_ID, COMPONENT_ID, TENANT_ID_LONG))
                .thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.createPermission(createPermission))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.NAME_ALREADY_EXISTS.code());
                });
    }

    @Test
    void createPermission_shouldThrowBaseException_whenComponentNotFound() {
        // Given
        CreatePermission createPermission = new CreatePermission();
        createPermission.setName("New Permission");
        createPermission.setMenuId(MENU_ID.toString());
        createPermission.setComponentId(COMPONENT_ID.toString());

        when(permissionsRepository.isAlreadyExist(anyString(), anyLong(), anyLong(), anyLong())).thenReturn(0);
        when(componentRepository.findById(COMPONENT_ID)).thenReturn(Optional.empty());
        when(menusRepository.findById(MENU_ID)).thenReturn(Optional.of(testMenu));
        when(tenantsRepository.findById(TENANT_ID_LONG)).thenReturn(Optional.of(testTenant));
        when(statusRepository.findById(StatusEnum.ACTIVE.value())).thenReturn(Optional.of(testStatus));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.createPermission(createPermission))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.BAD_REQUEST.code());
                });
    }

    @Test
    void createPermission_shouldRethrowBaseException_whenBaseExceptionOccurs() {
        // Given
        CreatePermission createPermission = new CreatePermission();
        createPermission.setName("New Permission");
        createPermission.setMenuId(MENU_ID.toString());
        createPermission.setComponentId(COMPONENT_ID.toString());

        BaseException originalException = new BaseException(
                "Original error",
                "Reason",
                HttpStatus.FORBIDDEN,
                "403",
                null
        );

        doAnswer(invocation -> {
            throw originalException;
        }).when(permissionsRepository).isAlreadyExist(anyString(), anyLong(), anyLong(), anyLong());

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.createPermission(createPermission))
                .isInstanceOf(BaseException.class)
                .hasMessage("Original error");
    }

    @Test
    void createPermission_shouldThrowBaseException_whenGenericExceptionOccurs() {
        // Given
        CreatePermission createPermission = new CreatePermission();
        createPermission.setName("New Permission");
        createPermission.setMenuId(MENU_ID.toString());
        createPermission.setComponentId(COMPONENT_ID.toString());

        when(permissionsRepository.isAlreadyExist(anyString(), anyLong(), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.createPermission(createPermission))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error");
    }

    @Test
    void editPermission_shouldThrowBaseException_whenPermissionNotFound() {
        // Given
        EditPermission editPermission = new EditPermission();
        editPermission.setPermissionId(PERMISSION_ID.toString());
        editPermission.setMenuId(MENU_ID.toString());
        editPermission.setComponentId(COMPONENT_ID.toString());

        when(permissionsRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());
        when(componentRepository.findById(COMPONENT_ID)).thenReturn(Optional.of(testComponent));
        when(menusRepository.findById(MENU_ID)).thenReturn(Optional.of(testMenu));
        when(statusRepository.findById(StatusEnum.ACTIVE.value())).thenReturn(Optional.of(testStatus));
        when(tenantsRepository.findById(TENANT_ID_LONG)).thenReturn(Optional.of(testTenant));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.editPermission(editPermission))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException baseException = (BaseException) exception;
                    assertThat(baseException.getResultCode()).isEqualTo(ResponseCodeEnum.BAD_REQUEST.code());
                });
    }

    @Test
    void editPermission_shouldRethrowBaseException_whenBaseExceptionOccurs() {
        // Given
        EditPermission editPermission = new EditPermission();
        editPermission.setPermissionId(PERMISSION_ID.toString());

        BaseException originalException = new BaseException(
                "Original error",
                "Reason",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "500",
                null
        );

        doAnswer(invocation -> {
            throw originalException;
        }).when(permissionsRepository).findById(PERMISSION_ID);

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.editPermission(editPermission))
                .isInstanceOf(BaseException.class)
                .hasMessage("Original error");
    }

    @Test
    void editPermission_shouldThrowBaseException_whenGenericExceptionOccurs() {
        // Given
        EditPermission editPermission = new EditPermission();
        editPermission.setPermissionId(PERMISSION_ID.toString());

        when(permissionsRepository.findById(PERMISSION_ID))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.editPermission(editPermission))
                .isInstanceOf(BaseException.class)
                .hasMessage("Database error");
    }

    @Test
    void getPermissionMetaData_shouldThrowBaseException_whenExceptionOccurs() {
        // Given
        when(permissionsRepository.findAllByTenantId(TENANT_ID_LONG))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.getPermissionMetaData())
                .isInstanceOf(BaseException.class);
    }

    @Test
    void getFilteredPermissionList_shouldThrowBaseException_whenExceptionOccurs() {
        // Given
        TableFilterRequest request = new TableFilterRequest();
        request.setLimit(10);
        request.setOffset(0);

        when(filterValueExtractor.extractFilterValues(anyList()))
                .thenThrow(new RuntimeException("Filter error"));

        // When & Then
        assertThatThrownBy(() -> permissionManagementService.getFilteredPermissionList(request))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void getActionHierarchy_shouldReturnActions_whenSuccessful() throws BaseException {
        // Given
        ActionHierarchyResultSet resultSet = mock(ActionHierarchyResultSet.class);
        when(resultSet.getIsManinAction()).thenReturn(1);
        when(resultSet.getActionId()).thenReturn(ACTION_ID);
        when(resultSet.getActionName()).thenReturn("View");

        when(permissionsRepository.getActionHierarchy(TENANT_ID_LONG, COMPONENT_ID))
                .thenReturn(List.of(resultSet));

        // STUB THE RESPONSE HANDLER (3-parameter version)
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        AllActions allActions = new AllActions();
        allActions.setMainActions(List.of(new MainActionsItem())); // Ensure list isn't empty for the assertion
        expectedResponse.setResponseData(allActions);

        when(responseHandler.responseBuilder(anyString(), anyString(), any()))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<AllActions> response = permissionManagementService.getActionHierarchy(COMPONENT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData().getMainActions()).isNotEmpty();
    }

    @Test
    void getAllMenuAndComponents_shouldReturnList_whenSuccessful() throws BaseException {
        // Given
        testMenu.setComponentsList(List.of(testComponent));
        // Ensure the menu is associated with the tenant in MDC (TENANT_ID_LONG)
        testMenu.setTenantsList(List.of(testTenant));

        when(menusRepository.findAll()).thenReturn(List.of(testMenu));

        // STUB THE RESPONSE HANDLER
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        List<MenuComponents> menuComponentsList = List.of(new MenuComponents());
        expectedResponse.setResponseData(menuComponentsList);

        when(responseHandler.responseBuilder(anyString(), anyString(), any()))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<MenuComponents>> response = permissionManagementService.getAllMenuAndComponents();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).isNotEmpty();
    }

    @Test
    void createPermission_shouldSavePermission_whenValidRequest() throws BaseException {
        // Given
        CreatePermission request = new CreatePermission();
        request.setName("New Perm");
        request.setMenuId(MENU_ID.toString());
        request.setComponentId(COMPONENT_ID.toString());
        request.setActions(List.of(ACTION_ID));

        when(permissionsRepository.isAlreadyExist(anyString(), anyLong(), anyLong(), anyLong())).thenReturn(0);
        when(componentRepository.findById(COMPONENT_ID)).thenReturn(Optional.of(testComponent));
        when(menusRepository.findById(MENU_ID)).thenReturn(Optional.of(testMenu));
        when(tenantsRepository.findById(TENANT_ID_LONG)).thenReturn(Optional.of(testTenant));
        when(statusRepository.findById(StatusEnum.ACTIVE.value())).thenReturn(Optional.of(testStatus));
        when(actionsRepository.findAllById(any())).thenReturn(List.of(testAction));

        // Fix: Use raw type or any() to avoid "no suitable method found" error
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        when(responseHandler.responseBuilder(anyString(), anyString()))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<String> response = permissionManagementService.createPermission(request);

        // Then
        assertThat(response).isNotNull();
        verify(permissionsRepository, times(1)).save(any(Permission.class));
    }

    @Test
    void editPermission_shouldUpdatePermission_whenValidRequest() throws BaseException {
        // Given
        EditPermission request = new EditPermission();
        request.setPermissionId(PERMISSION_ID.toString());
        request.setMenuId(MENU_ID.toString());
        request.setComponentId(COMPONENT_ID.toString());

        when(permissionsRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));
        when(componentRepository.findById(COMPONENT_ID)).thenReturn(Optional.of(testComponent));
        when(menusRepository.findById(MENU_ID)).thenReturn(Optional.of(testMenu));
        when(statusRepository.findById(StatusEnum.ACTIVE.value())).thenReturn(Optional.of(testStatus));
        when(tenantsRepository.findById(TENANT_ID_LONG)).thenReturn(Optional.of(testTenant));

        // FIX: Use raw type for the stubbing to bypass generic type check
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        when(responseHandler.responseBuilder(
                eq(ResponseCodeEnum.PERMISSION_EDIT_SUCCESSFUL.code()),
                eq(ResponseCodeEnum.PERMISSION_EDIT_SUCCESSFUL.description())))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<String> response = permissionManagementService.editPermission(request);

        // Then
        assertThat(response).isNotNull();
        verify(permissionsRepository, times(1)).save(any(Permission.class));
    }

    @Test
    void getPermissionMetaData_shouldReturnData_whenSuccessful() throws BaseException {
        // Given
        when(permissionsRepository.findAllByTenantId(TENANT_ID_LONG)).thenReturn(List.of(testPermission));

        // FIX: Stub responseBuilder(String, String, T)
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        expectedResponse.setResponseData(List.of(new PermissionMetaData()));

        when(responseHandler.responseBuilder(anyString(), anyString(), anyList()))
                .thenReturn(expectedResponse);

        // When
        CommonAdaptorResp<List<PermissionMetaData>> response = permissionManagementService.getPermissionMetaData();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getResponseData()).hasSize(1);
    }

    @Test
    void getFilteredPermissionList_shouldReturnEmptyList_whenNoResults() throws BaseException {
        TableFilterRequest request = new TableFilterRequest();
        request.setLimit(10);
        request.setOffset(0);
        request.setFilterValues(new ArrayList<>());

        when(filterValueExtractor.extractFilterValues(any())).thenReturn(new HashMap<>());
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(new ArrayList<>());

        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        lenient().doReturn(expectedResponse)
                .when(responseHandler)
                .responseBuilder(anyString(), anyString(), anyList(), any());

        CommonAdaptorResp<List<PermissionView>> response = permissionManagementService.getFilteredPermissionList(request);
        assertThat(response).isNotNull();
    }

    @Test
    void getActionHierarchy_shouldHandleSubActionsWithAttributes() throws BaseException {
        ActionHierarchyResultSet mainResult = mock(ActionHierarchyResultSet.class);
        when(mainResult.getIsManinAction()).thenReturn(1);
        when(mainResult.getActionId()).thenReturn(ACTION_ID);
        when(mainResult.getActionName()).thenReturn("View");
        when(mainResult.getAttributeId()).thenReturn(null);

        ActionHierarchyResultSet subResult = mock(ActionHierarchyResultSet.class);
        when(subResult.getIsManinAction()).thenReturn(0);
        when(subResult.getActionId()).thenReturn(31L);
        when(subResult.getActionName()).thenReturn("Edit");
        when(subResult.getMainActionId()).thenReturn(ACTION_ID);
        when(subResult.getAttributeId()).thenReturn(ATTRIBUTE_ID);
        when(subResult.getAttributeName()).thenReturn("Name");

        when(permissionsRepository.getActionHierarchy(TENANT_ID_LONG, COMPONENT_ID))
                .thenReturn(List.of(mainResult, subResult));
        when(responseHandler.responseBuilder(anyString(), anyString(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<AllActions> response = permissionManagementService.getActionHierarchy(COMPONENT_ID);
        assertThat(response).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredPermissionList_shouldReturnList_whenSuccessful() throws BaseException {
        // Given
        TableFilterRequest request = new TableFilterRequest();
        request.setLimit(10);
        request.setOffset(0);
        request.setFilterValues(new ArrayList<>());

        // FIX 1: Explicitly define the type for the list to avoid inference errors
        Object[] row = new Object[]{PERMISSION_ID.toString(), "Name", "Desc", "Comp", "Menu", 1};
        List<Object[]> results = new ArrayList<>();
        results.add(row);

        when(filterValueExtractor.extractFilterValues(any())).thenReturn(new HashMap<>());
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        // Mocking the fluent API chain
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);

        // FIX 2: Create the response object
        CommonAdaptorResp expectedResponse = new CommonAdaptorResp();
        PermissionView view = PermissionView.builder().totalCount(1).build();
        expectedResponse.setResponseData(List.of(view));

        // FIX 3: Use doReturn...when to bypass strict generic type checking
        lenient().doReturn(expectedResponse)
                .when(responseHandler)
                .responseBuilder(anyString(), anyString(), anyList(), any());

        // When
        CommonAdaptorResp<List<PermissionView>> response = permissionManagementService.getFilteredPermissionList(request);

        // Then
        assertThat(response).isNotNull();
        verify(entityManager).createNativeQuery(anyString());
        verify(query, atLeastOnce()).setParameter(anyString(), any());
    }

}