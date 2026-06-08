package com.adl.et.telco.crm.usermanagerservice.serviceinterface.authentication;

import com.adl.et.telco.crm.usermanagerservice.dto.authentication.AttributeDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.authentication.UserBasicInfo;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.user.UserAuthDTO;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import java.util.List;

public interface UserDetailsServiceInterface {
    CommonAdaptorResp<UserBasicInfo> getBasicInfoByUserName(String userName) throws BaseException;

    CommonAdaptorResp<List<AttributeDetails>> getAttributeDetails() throws BaseException;

    CommonAdaptorResp<String> getUsername(UserAuthDTO userAuthDTO) throws BaseException;
}

