package com.adl.et.telco.crm.usermanagerservice.serviceinterface.user;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.ActionLogData;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.SingleActionLogResponse;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.ActionLog;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import java.util.List;

public interface UserActionLogServiceInterface {
    CommonAdaptorResp<String> logUserActivityLog(ActionLog actionLogRequest) throws BaseException;
    CommonAdaptorResp<String> updateUserActivityLog(String activityId, String status, String statusDescription) throws BaseException;

    CommonAdaptorResp<List<ActionLogData>> getActionLog(TableFilterRequest actionLogRequest) throws BaseException;

    CommonAdaptorResp<SingleActionLogResponse> getActionLogById(Long id) throws BaseException;
}

