package com.adl.et.telco.crm.usermanagerservice.serviceinterface.authentication;

import com.adl.et.telco.crm.usermanagerservice.dto.authentication.ExternalUserResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

public interface ExternalUserAuthenticateServiceInterface {
    CommonAdaptorResp<ExternalUserResponse> getExternalUserInfo(String userName) throws BaseException;
}


