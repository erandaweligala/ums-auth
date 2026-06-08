package com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication;

import com.adl.et.telco.crm.usermanagerservice.dto.authentication.ExternalUserResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.authentication.ExternalUserAuthenticateServiceInterface;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExternalUserAuthenticateService implements ExternalUserAuthenticateServiceInterface {

    @Autowired
    private ResponseHandler responseHandler;

    @Autowired
    private CrmUserRepository crmUserRepository;

    @Override
    public CommonAdaptorResp<ExternalUserResponse> getExternalUserInfo(String userName) throws BaseException {

        try {
            ExternalUserResponse externalUserResponse = crmUserRepository.findByUserName(userName);
            if (externalUserResponse != null)
                return responseHandler.responseBuilder( ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(),externalUserResponse);
            else
                throw new BaseException(ResponseCodeEnum.INVALID_USER.description(), ResponseCodeEnum.INVALID_USER.description(), HttpStatus.FORBIDDEN, ResponseCodeEnum.INVALID_USER.code(), null);

        } catch (BaseException e) {
            throw new BaseException(e.getMessage(), e.getReason(), e.getHttpStatus(), e.getResultCode(), e.getStackTraceElements());

        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }
}

