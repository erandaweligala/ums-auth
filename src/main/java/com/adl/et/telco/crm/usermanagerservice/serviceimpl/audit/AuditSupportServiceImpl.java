package com.adl.et.telco.crm.usermanagerservice.serviceimpl.audit;



import com.adl.et.telco.crm.usermanagerservice.dto.audit.ActionDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Actions;
import com.adl.et.telco.crm.usermanagerservice.repository.ActionsRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.audit.AuditSupportInterface;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuditSupportServiceImpl implements AuditSupportInterface {

    @Autowired
    private ActionsRepository actionsRepository;
    @Autowired
    private ResponseHandler responseHandler;
    @Autowired
    private CrmUserRepository crmUserRepository;

    @Override
    public CommonAdaptorResp<ActionDetails> getActionDetails(Long id) throws BaseException {
        try {
            Optional<Actions> actions = actionsRepository.findById(id);
            if (actions.isPresent()) {
                Actions act = actions.get();
                ActionDetails actionDetails = ActionDetails.builder().id(act.getId()).name(act.getName()).description(act.getDescription()).build();
                return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), actionDetails);
            } else
                throw new BaseException(ResponseCodeEnum.INVALID_ACTION.description(), ResponseCodeEnum.INVALID_ACTION.description(), HttpStatus.BAD_REQUEST, ResponseCodeEnum.INVALID_ACTION.code(), null);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<String>> getAllUsers() throws BaseException {
        try {
            List<String> users = new ArrayList<>();
            users = crmUserRepository.findByTenantId(Long.parseLong(MDC.get(Constants.TENANT_ID)));
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), users);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<ActionDetails>> getActions() throws BaseException {
        try {
            List<ActionDetails> actions = actionsRepository.getActions(Long.parseLong(MDC.get(Constants.TENANT_ID)));
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), actions);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }
}

