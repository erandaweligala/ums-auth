package com.adl.et.telco.crm.usermanagerservice.serviceinterface.permission;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.*;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import java.util.List;

public interface PermissionManagementInterface {
    CommonAdaptorResp<List<PermissionView>> getPermissionList(String permissionName, Long menuId, Long sectionId, int limit, int offset) throws BaseException;

    CommonAdaptorResp<PermissionDetails> getPermissionDetails(Long permissionId) throws BaseException;

    CommonAdaptorResp<AllActions> getActionHierarchy(Long componentId) throws BaseException;

    CommonAdaptorResp<List<MenuComponents>> getAllMenuAndComponents() throws BaseException;

    CommonAdaptorResp<String> createPermission(CreatePermission createPermission) throws BaseException;

    CommonAdaptorResp<String> editPermission(EditPermission editPermission) throws BaseException;

    CommonAdaptorResp<List<PermissionMetaData>> getPermissionMetaData() throws BaseException;

    CommonAdaptorResp<List<PermissionView>> getFilteredPermissionList(TableFilterRequest tableFilterRequest) throws BaseException;
}

