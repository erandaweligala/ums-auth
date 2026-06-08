package com.adl.et.telco.crm.usermanagerservice.controller;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.role.CreateNewRole;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleView;
import com.adl.et.telco.crm.usermanagerservice.dto.role.UpdateRole;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.role.RoleManagementInterface;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/ums/role")
@Tag(name = "Role Management", description = "Operations for managing roles")
public class RoleManagementController extends BaseController {

    @Autowired
    private RoleManagementInterface roleManagementInterface;

    // routeid-142 actionid-213
    @GetMapping("")
    public ResponseEntity<CommonAdaptorResp<List<RoleView>>> getRoleList(@RequestParam int limit,
            @RequestParam int offset, @RequestParam(required = false) String roleName,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(roleManagementInterface.getRoleList(limit, offset, roleName));
    }

    // routeid-141 actionid-212
    @GetMapping("/{roleId}")
    public ResponseEntity<CommonAdaptorResp<RoleDetails>> getRoleDetails(@PathVariable Long roleId,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(roleManagementInterface.getRoleDetails(roleId));
    }

    // routeid-138 actionid-209
    @PostMapping("")
    public ResponseEntity<CommonAdaptorResp<String>> createRole(@RequestBody CreateNewRole createNewRoleRequest,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(roleManagementInterface.createRole(createNewRoleRequest));
    }

    // routeid-139 actionid-210
    @PatchMapping("")
    public ResponseEntity<CommonAdaptorResp<String>> updateRole(@RequestBody UpdateRole updateRoleRequest,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(roleManagementInterface.updateRole(updateRoleRequest));
    }

    // routeid-127 actionid-198
    @GetMapping("/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<MetaData>>> getRoleMetaData(HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(roleManagementInterface.getRoleMetaData());
    }

    // routeid-140 actionid-211
    @PostMapping("/role-list")
    public ResponseEntity<CommonAdaptorResp<List<RoleView>>> getFilteredUserList(
            @RequestBody TableFilterRequest tableFilterRequest, HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(roleManagementInterface.getFilteredUserList(tableFilterRequest));
    }

}
