package com.adl.et.telco.crm.usermanagerservice.serviceimpl.role;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.role.*;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.filter.FilterValueExtractor;
import com.adl.et.telco.crm.usermanagerservice.mapper.common.MySQLEventMapper;
import com.adl.et.telco.crm.usermanagerservice.mapper.role.RoleMapper;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Roles;
import com.adl.et.telco.crm.usermanagerservice.repository.PermissionsRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.RoleRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.TenantsRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.permission.PermissionManagementServiceImpl;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.role.RoleManagementInterface;
import com.adl.et.telco.crm.usermanagerservice.util.PageUtils;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleManagementManagementServiceImpl implements RoleManagementInterface {

    private final RoleRepository                  repository;
    private final TenantsRepository               tenantsRepository;
    private final PermissionsRepository           permissionsRepository;
    private final ResponseHandler                 responseHandler;
    private final RoleMapper                      roleMapper;
    private final PermissionManagementServiceImpl permissionManagementService;
    private final FilterValueExtractor            filterValueExtractor;
    private final MySQLEventMapper                mysqlEventMapper;  // ← Kafka publisher

    // =========================================================================
    // Read-only methods — UNCHANGED
    // =========================================================================

    @Override
    public CommonAdaptorResp<List<MetaData>> getRoleMetaData() throws BaseException {
        try {
            List<MetaData> roleList = repository.findRoleListMetaData(Long.parseLong(MDC.get(Constants.TENANT_ID)));
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), roleList);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<RoleView>> getRoleList(int limit, int offset, String roleName) throws BaseException {
        try {
            List<RoleView> roleList = repository.findRoleListView(Long.parseLong(MDC.get(Constants.TENANT_ID)), limit, offset, roleName);
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), roleList,
                    PageUtils.buildPageDetails(limit, offset, !roleList.isEmpty() ? roleList.get(0).getTotalCount() : 0, roleList.size()));
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<RoleDetails> getRoleDetails(Long roleId) throws BaseException {
        try {
            Optional<Roles> role = repository.findByIdAndTenantId(roleId, Long.parseLong(MDC.get(Constants.TENANT_ID)));
            List<MenuComponents> menuComponents = permissionManagementService.getAllMenuAndComponents().getResponseData();
            if (role.isPresent()) {
                List<RoleDetailsRepoDTO> roleList = repository.getRoleDetails(roleId, Long.parseLong(MDC.get(Constants.TENANT_ID)));
                RoleDetails roleDetails = roleMapper.mapRoleDetails(roleList, role.get(), menuComponents);
                return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), roleDetails);
            }
            throw new BaseException(ResponseCodeEnum.INVALID_USER.description(), ResponseCodeEnum.INVALID_USER.description(), HttpStatus.BAD_REQUEST, ResponseCodeEnum.INVALID_USER.code(), null);
        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // createRole  ← CHANGED: no repository.save(); uses Kafka instead
    // =========================================================================

    @Override
    public CommonAdaptorResp<String> createRole(CreateNewRole newRole) throws BaseException {
        try {
            long currentTenantId = Long.parseLong(MDC.get(Constants.TENANT_ID));

            // Duplicate name check (read-only)
            long nameAlreadyExistCount = repository.isNameAlreadyExist(newRole.getRoleName(), currentTenantId);
            if (nameAlreadyExistCount != 0) {
                throw new BaseException(
                        ResponseCodeEnum.NAME_ALREADY_EXISTS.description(),
                        ResponseCodeEnum.NAME_ALREADY_EXISTS.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.NAME_ALREADY_EXISTS.code(),
                        null
                );
            }

            // Validate tenant exists (read-only)
            if (tenantsRepository.findById(currentTenantId).isEmpty()) {
                throw new BaseException(
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.BAD_REQUEST.code(),
                        null
                );
            }

            List<Long> permissionIds = newRole.getPermissionIdList() != null
                    ? newRole.getPermissionIdList()
                    : List.of();

            // actor: use createdBy from request if present, otherwise fall back to roleName
            String actor = (newRole.getCreatedBy() != null && !newRole.getCreatedBy().isBlank())
                    ? newRole.getCreatedBy()
                    : newRole.getRoleName();

            // ── Kafka publish (replaces repository.save) ──────────────────
            mysqlEventMapper.publishCreateRole(newRole, permissionIds, currentTenantId, actor);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.ROLE_CREATION_SUCCESSFUL.code(),
                    ResponseCodeEnum.ROLE_CREATION_SUCCESSFUL.description()
            );

        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // updateRole  ← CHANGED: no repository.save(); uses Kafka instead
    // =========================================================================

    @Override
    public CommonAdaptorResp<String> updateRole(UpdateRole updateRoleRequest) throws BaseException {
        try {
            long currentTenantId = Long.parseLong(MDC.get(Constants.TENANT_ID));

            // Validate role exists (read-only)
            Optional<Roles> role = repository.findByIdAndTenantId(
                    Long.parseLong(updateRoleRequest.getRoleId()), currentTenantId);
            if (role.isEmpty()) {
                throw new BaseException(
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.BAD_REQUEST.code(),
                        null
                );
            }

            List<Long> permissionIds = (updateRoleRequest.getPermissionIdList() != null
                    && !updateRoleRequest.getPermissionIdList().isEmpty())
                    ? updateRoleRequest.getPermissionIdList()
                    : List.of();

            // actor: use createdBy field from request (edit requests often carry this)
            String actor = (updateRoleRequest.getCreatedBy() != null && !updateRoleRequest.getCreatedBy().isBlank())
                    ? updateRoleRequest.getCreatedBy()
                    : role.get().getName();

            // ── Kafka publish (replaces repository.save) ──────────────────
            mysqlEventMapper.publishEditRole(updateRoleRequest, permissionIds, actor);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.ROLE_EDIT_SUCCESSFUL.code(),
                    ResponseCodeEnum.ROLE_EDIT_SUCCESSFUL.description()
            );

        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // getFilteredUserList — UNCHANGED
    // =========================================================================

    public CommonAdaptorResp<List<RoleView>> getFilteredUserList(TableFilterRequest tableFilterRequest) throws BaseException {
        try {
            Map<String, String> filterDataMap = filterValueExtractor.extractFilterValues(tableFilterRequest.getFilterValues());
            List<RoleView> roleList = repository.getFilteredUserList(
                    filterDataMap.get("roleId"),       filterDataMap.get("roleId" + Constants.FILTER_TYPE),
                    filterDataMap.get("name"),         filterDataMap.get("name" + Constants.FILTER_TYPE),
                    filterDataMap.get("description"),  filterDataMap.get("description" + Constants.FILTER_TYPE),
                    tableFilterRequest.getLimit(),     tableFilterRequest.getOffset(),
                    Long.parseLong(MDC.get(Constants.TENANT_ID)));
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), roleList,
                    PageUtils.buildPageDetails(tableFilterRequest.getLimit(), tableFilterRequest.getOffset(),
                            !roleList.isEmpty() ? roleList.get(0).getTotalCount() : 0, roleList.size()));
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }
}