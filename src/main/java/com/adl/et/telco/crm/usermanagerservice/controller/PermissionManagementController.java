package com.adl.et.telco.crm.usermanagerservice.controller;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.*;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents.MenuComponents;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.permission.PermissionManagementInterface;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ums/permission")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "Permission Management")
public class PermissionManagementController extends BaseController {

    private final PermissionManagementInterface permissionManagementInterface;

    //routeid-150 actionid-221
    @GetMapping("")
    public ResponseEntity<CommonAdaptorResp<List<PermissionView>>> getPermissionList(
            @RequestParam(required = false) String permissionName,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) Long componentId,
            @RequestParam() int limit,
            @RequestParam() int offset, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.getPermissionList(permissionName, menuId, componentId, limit, offset));
    }

    //routeid-145 actionid-216
    @GetMapping("/{permissionId}")
    public ResponseEntity<CommonAdaptorResp<PermissionDetails>> getPermissionDetails(
            @PathVariable Long permissionId, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.getPermissionDetails(permissionId));
    }

    //routeid-147 actionid-218
    @GetMapping("/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<PermissionMetaData>>> getPermissionMetaData( HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.getPermissionMetaData());
    }

    //routeid-149 actionid-210
    @GetMapping("/action-hierarchy-for-create-permission")
    public ResponseEntity<CommonAdaptorResp<AllActions>> getActionHierarchy(
            @RequestParam Long componentId, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.getActionHierarchy(componentId));
    }

    //routeid-148 actionid-219
    @GetMapping("/menu-components")
    public ResponseEntity<CommonAdaptorResp<List<MenuComponents>>> getAllMenuAndComponents(HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.getAllMenuAndComponents());
    }

    //routeid-143 actionid-214
    @PostMapping("")
    public ResponseEntity<CommonAdaptorResp<String>> createPermission(@RequestBody CreatePermission createPermission, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.createPermission(createPermission));
    }

    //routeid-144 actionid-215
    @PatchMapping("")
    public ResponseEntity<CommonAdaptorResp<String>> editPermission(@RequestBody EditPermission editPermission, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.editPermission(editPermission));
    }

    //routeid-146 actionid-217
    @PostMapping("/permission-list")
    public ResponseEntity<CommonAdaptorResp<List<PermissionView>>> getFilteredPermissionList(@RequestBody TableFilterRequest tableFilterRequest, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(permissionManagementInterface.getFilteredPermissionList(tableFilterRequest));
    }
}

