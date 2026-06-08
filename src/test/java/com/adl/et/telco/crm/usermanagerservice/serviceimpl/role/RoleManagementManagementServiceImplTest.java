package com.adl.et.telco.crm.usermanagerservice.serviceimpl.role;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.role.*;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.filter.FilterValueExtractor;
import com.adl.et.telco.crm.usermanagerservice.mapper.role.RoleMapper;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Permission;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Roles;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Tenants;
import com.adl.et.telco.crm.usermanagerservice.repository.PermissionsRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.RoleRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.TenantsRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.permission.PermissionManagementServiceImpl;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoleManagementManagementServiceImplTest {

    @InjectMocks
    private RoleManagementManagementServiceImpl service;

    @Mock private RoleRepository roleRepository;
    @Mock private TenantsRepository tenantsRepository;
    @Mock private PermissionsRepository permissionsRepository;
    @Mock private ResponseHandler responseHandler;
    @Mock private RoleMapper roleMapper;
    @Mock private PermissionManagementServiceImpl permissionService;
    @Mock private FilterValueExtractor filterValueExtractor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MDC.put(Constants.TENANT_ID, "1");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // -------- getRoleMetaData --------

    @Test
    void getRoleMetaData_success() throws BaseException {
        when(roleRepository.findRoleListMetaData(1L))
                .thenReturn(List.of(new MetaData("a", "b")));
        when(responseHandler.responseBuilder(any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<MetaData>> resp = service.getRoleMetaData();

        assertThat(resp).isNotNull();
    }

    @Test
    void getRoleMetaData_exception() {
        when(roleRepository.findRoleListMetaData(anyLong()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> service.getRoleMetaData())
                .isInstanceOf(BaseException.class);
    }

    // -------- getRoleList --------

    @Test
    void getRoleList_success() throws BaseException {
        RoleView view = new RoleView("1", "ADMIN", "desc", 1);
        when(roleRepository.findRoleListView(anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(view));
        when(responseHandler.responseBuilder(any(), any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<RoleView>> resp =
                service.getRoleList(10, 0, "ADMIN");

        assertThat(resp).isNotNull();
    }

    // -------- getRoleDetails --------

    @Test
    void getRoleDetails_success() throws BaseException {
        Roles role = new Roles();
        when(roleRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(role));
        when(roleRepository.getRoleDetails(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(permissionService.getAllMenuAndComponents())
                .thenReturn(CommonAdaptorResp.<List<MenuComponents>>builder()
                        .responseData(List.of())
                        .build());
        when(roleMapper.mapRoleDetails(any(), any(), any()))
                .thenReturn(new RoleDetails());
        when(responseHandler.responseBuilder(any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<RoleDetails> resp = service.getRoleDetails(1L);

        assertThat(resp).isNotNull();
    }

    @Test
    void getRoleDetails_roleNotFound() {
        when(roleRepository.findByIdAndTenantId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRoleDetails(1L))
                .isInstanceOf(BaseException.class);
    }

    // -------- createRole --------

    @Test
    void createRole_success() throws BaseException {
        CreateNewRole req = new CreateNewRole();
        req.setRoleName("ADMIN");
        req.setPermissionIdList(List.of(1L));

        when(roleRepository.isNameAlreadyExist(any(), anyLong())).thenReturn(0L);
        when(permissionsRepository.findAllById(any())).thenReturn(List.of(new Permission()));
        when(tenantsRepository.findById(anyLong())).thenReturn(Optional.of(new Tenants()));
        when(responseHandler.responseBuilder(any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<String> resp = service.createRole(req);

        assertThat(resp).isNotNull();
        verify(roleRepository).save(any());
    }

    @Test
    void createRole_nameExists() {
        CreateNewRole req = new CreateNewRole();
        req.setRoleName("ADMIN");

        when(roleRepository.isNameAlreadyExist(any(), anyLong()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.createRole(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void createRole_tenantMissing() {
        CreateNewRole req = new CreateNewRole();
        req.setRoleName("ADMIN");
        req.setPermissionIdList(List.of());

        when(roleRepository.isNameAlreadyExist(any(), anyLong())).thenReturn(0L);
        when(tenantsRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRole(req))
                .isInstanceOf(BaseException.class);
    }

    // -------- updateRole --------

    @Test
    void updateRole_success() throws BaseException {
        UpdateRole req = new UpdateRole();
        req.setRoleId("1");
        req.setPermissionIdList(List.of(1L));

        when(roleRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(new Roles()));
        when(permissionsRepository.findAllById(any()))
                .thenReturn(List.of(new Permission()));
        when(responseHandler.responseBuilder(any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<String> resp = service.updateRole(req);

        assertThat(resp).isNotNull();
    }

    @Test
    void updateRole_roleNotFound() {
        UpdateRole req = new UpdateRole();
        req.setRoleId("1");

        when(roleRepository.findByIdAndTenantId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole(req))
                .isInstanceOf(BaseException.class);
    }

    // -------- getFilteredUserList --------

    @Test
    void getFilteredUserList_success() throws BaseException {
        TableFilterRequest req = new TableFilterRequest();
        req.setLimit(10);
        req.setOffset(0);

        when(filterValueExtractor.extractFilterValues(any()))
                .thenReturn(Map.of());
        when(roleRepository.getFilteredUserList(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyLong()))
                .thenReturn(List.of(new RoleView("1", "ADMIN", "desc", 1)));
        when(responseHandler.responseBuilder(any(), any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<RoleView>> resp = service.getFilteredUserList(req);

        assertThat(resp).isNotNull();
    }

    // ---- getRoleList empty list branch ----

    @Test
    void getRoleList_emptyList() throws BaseException {
        when(roleRepository.findRoleListView(anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());
        when(responseHandler.responseBuilder(any(), any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<RoleView>> resp = service.getRoleList(10, 0, null);
        assertThat(resp).isNotNull();
    }

    // ---- updateRole with empty permissionIdList ----

    @Test
    void updateRole_withEmptyPermissionIdList() throws BaseException {
        UpdateRole req = new UpdateRole();
        req.setRoleId("1");
        req.setPermissionIdList(Collections.emptyList());

        when(roleRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(new Roles()));
        when(responseHandler.responseBuilder(any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<String> resp = service.updateRole(req);
        assertThat(resp).isNotNull();
        verify(permissionsRepository, never()).findAllById(any());
    }

    // ---- getFilteredUserList empty result ----

    @Test
    void getFilteredUserList_emptyResult() throws BaseException {
        TableFilterRequest req = new TableFilterRequest();
        req.setLimit(10);
        req.setOffset(0);

        when(filterValueExtractor.extractFilterValues(any())).thenReturn(Map.of());
        when(roleRepository.getFilteredUserList(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyLong()))
                .thenReturn(List.of());
        when(responseHandler.responseBuilder(any(), any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<RoleView>> resp = service.getFilteredUserList(req);
        assertThat(resp).isNotNull();
    }

    // ---- getFilteredUserList exception path ----

    @Test
    void getFilteredUserList_exception() {
        TableFilterRequest req = new TableFilterRequest();
        req.setLimit(10);
        req.setOffset(0);

        when(filterValueExtractor.extractFilterValues(any()))
                .thenThrow(new RuntimeException("filter error"));

        assertThatThrownBy(() -> service.getFilteredUserList(req))
                .isInstanceOf(BaseException.class);
    }

    // ---- getRoleDetails general exception path ----

    @Test
    void getRoleDetails_generalException() {
        when(roleRepository.findByIdAndTenantId(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> service.getRoleDetails(1L))
                .isInstanceOf(BaseException.class);
    }

    // ---- createRole when roles empty (findAllById returns empty) ----

    @Test
    void createRole_withEmptyPermissions_success() throws BaseException {
        CreateNewRole req = new CreateNewRole();
        req.setRoleName("VIEWER");
        req.setPermissionIdList(Collections.emptyList());

        when(roleRepository.isNameAlreadyExist(any(), anyLong())).thenReturn(0L);
        when(permissionsRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(tenantsRepository.findById(anyLong())).thenReturn(Optional.of(new Tenants()));
        when(responseHandler.responseBuilder(any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<String> resp = service.createRole(req);
        assertThat(resp).isNotNull();
        verify(roleRepository).save(any());
    }
}
