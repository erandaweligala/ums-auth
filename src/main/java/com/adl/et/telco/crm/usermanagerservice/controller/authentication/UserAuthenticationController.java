package com.adl.et.telco.crm.usermanagerservice.controller.authentication;

import com.adl.et.telco.crm.usermanagerservice.controller.BaseController;
import com.adl.et.telco.crm.usermanagerservice.dto.authentication.AttributeDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.authentication.ExternalUserResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.authentication.UserBasicInfo;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.user.UserAuthDTO;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.authentication.ExternalUserAuthenticateServiceInterface;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.authentication.UserDetailsServiceInterface;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ums/auth-user")
@RequiredArgsConstructor
@Tag(name = "User Authentication", description = "Operations for user authentication and external user info")
public class UserAuthenticationController extends BaseController {

    private final ExternalUserAuthenticateServiceInterface externalUserAuthenticateServiceInterface;
    private final UserDetailsServiceInterface userDetailsServiceInterface;

    @GetMapping("/external/user/{userName}")
    public ResponseEntity<CommonAdaptorResp<ExternalUserResponse>> getExternalUserInfo(@PathVariable String userName,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(externalUserAuthenticateServiceInterface.getExternalUserInfo(userName));
    }

    @GetMapping(value = "basic-info/{userName}")
    public ResponseEntity<CommonAdaptorResp<UserBasicInfo>> getBasicInfoByUserName(@PathVariable String userName,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userDetailsServiceInterface.getBasicInfoByUserName(userName));
    }

    @GetMapping(value = "attribute-info")
    public ResponseEntity<CommonAdaptorResp<List<AttributeDetails>>> getAttributeDetails(
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userDetailsServiceInterface.getAttributeDetails());
    }

    @PostMapping("/get-username")
    public ResponseEntity<CommonAdaptorResp<String>> getUserName(@RequestBody UserAuthDTO userAuthDTO,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userDetailsServiceInterface.getUsername(userAuthDTO));
    }
}
