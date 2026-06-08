package com.adl.et.telco.crm.usermanagerservice.serviceinterface.role;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.role.CreateNewRole;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleView;
import com.adl.et.telco.crm.usermanagerservice.dto.role.UpdateRole;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import java.util.List;

public interface RoleManagementInterface {
    CommonAdaptorResp<List<MetaData>> getRoleMetaData() throws BaseException;

    CommonAdaptorResp<List<RoleView>> getRoleList(int limit, int offset,String roleName) throws BaseException;

    CommonAdaptorResp<RoleDetails> getRoleDetails(Long roleId) throws BaseException;

    CommonAdaptorResp<String> createRole(CreateNewRole createNewRoleRequest) throws BaseException;

    CommonAdaptorResp<String> updateRole(UpdateRole updateRoleRequest) throws BaseException;

    CommonAdaptorResp<List<RoleView>> getFilteredUserList(TableFilterRequest tableFilterRequest) throws BaseException;
}

