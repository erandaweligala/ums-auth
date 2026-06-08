package com.adl.et.telco.crm.usermanagerservice.controller;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.user.*;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.user.UserManagementInterface;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/ums/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations for managing users")
public class UserManagementController extends BaseController {

    private final UserManagementInterface userManagementInterface;

    // routeid-137 actionid-208
    @GetMapping("")
    public ResponseEntity<CommonAdaptorResp<List<AllUserDetails>>> getAllUsers(
            @RequestParam int limit,
            @RequestParam int offset,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long statusId,
            HttpServletRequest httpServletRequest) throws BaseException {

        if (userName != null) {
            userName = URLDecoder.decode(userName, StandardCharsets.UTF_8)
                    .replace("+", " ")  // handle '+' encoded spaces too
                    .trim();
        }

        return setResponseEntity(userManagementInterface.getAllUsers(limit, offset, userName, roleId, statusId));
    }

    // routeid-136 actionid-207
    @PostMapping("/user-list")
    public ResponseEntity<CommonAdaptorResp<List<AllUserDetails>>> getAllFilteredUserList(
            @RequestBody TableFilterRequest tableFilterRequest, HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(userManagementInterface.getAllFilteredUserList(tableFilterRequest));
    }

    // routeid-134 actionid-205
    @GetMapping("/{userId}")
    public ResponseEntity<CommonAdaptorResp<UserDetails>> getUserDetails(@PathVariable Long userId,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.getUserDetails(userId));
    }

    @GetMapping("/get-details-by-email/{email}")
    public ResponseEntity<CommonAdaptorResp<UserDetails>> findUserByEmail(@PathVariable String email,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.getUserDetailsByEmail(email));
    }

    // routeid-126 actionid-197
    @PostMapping("")
    public ResponseEntity<CommonAdaptorResp<String>> createUser(@RequestBody CreateUserRequest newUser,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.createUser(newUser));
    }

    // routeid-135 actionid-206
    @PostMapping("/validate")
    public ResponseEntity<CommonAdaptorResp<EmailValidateResponse>> validateEmail(
            @RequestBody EmailValidationRequest emailValidationRequest, HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(userManagementInterface.validateEmail(emailValidationRequest));
    }

    @PostMapping("/expire/{days}")
    public ResponseEntity<CommonAdaptorResp<String>> expire(@PathVariable Integer days,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.expire(days));
    }

    // routeid-133 actionid-204
    @PatchMapping("")
    public ResponseEntity<CommonAdaptorResp<String>> editUser(@RequestBody EditUserRequest user,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.editUser(user));
    }

    // routeid-128 actionid-199
    @GetMapping("/status/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<MetaData>>> getUserStatusMetaData(
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.getUserStatusMetaData());
    }

    // routeid-129 actionid-200
    @GetMapping("/user-account/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<MetaData>>> getUserAccountMetaData(
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.getUserAccountMetaData());
    }

    // routeid-130 actionid-201
    @GetMapping("/user-type/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<MetaData>>> getUserTypeMetaData(HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(userManagementInterface.getUserTypeMetaData());
    }

    // routeid-131 actionid-202
    @GetMapping("/default-group/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<MetaData>>> getDefaultGroupMetaData(
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userManagementInterface.getDefaultGroupMetaData());
    }

    // routeid-132 actionid-203
    @GetMapping("/user-group/meta-data")
    public ResponseEntity<CommonAdaptorResp<List<MetaData>>> getUserGroupMetaData(HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(userManagementInterface.getUserGroupMetaData());
    }

}
