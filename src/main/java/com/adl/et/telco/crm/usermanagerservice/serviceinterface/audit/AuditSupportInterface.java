package com.adl.et.telco.crm.usermanagerservice.serviceinterface.audit;

import com.adl.et.telco.crm.usermanagerservice.dto.audit.ActionDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import java.util.List;

public interface AuditSupportInterface {
    CommonAdaptorResp<ActionDetails> getActionDetails(Long id) throws BaseException;

    CommonAdaptorResp<List<String>> getAllUsers() throws BaseException;

    CommonAdaptorResp<List<ActionDetails>> getActions() throws BaseException;
}

