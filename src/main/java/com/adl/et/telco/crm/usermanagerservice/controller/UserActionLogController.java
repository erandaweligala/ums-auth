package com.adl.et.telco.crm.usermanagerservice.controller;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.ActionLogData;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.CreateActionLogRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.SingleActionLogResponse;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.ActionLog;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.user.UserActionLogServiceInterface;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ums/user-activity-log")
@RequiredArgsConstructor
@Tag(name = "User Activity Log", description = "Operations for logging and retrieving user activities")
public class UserActionLogController extends BaseController {

    private final UserActionLogServiceInterface userActionLogServiceInterface;
    private final ObjectMapper mapper;

    @PostMapping("/create-log")
    public ResponseEntity<CommonAdaptorResp<String>> logUserActivity(
            @RequestBody CreateActionLogRequest userActionLogRequest, HttpServletRequest httpServletRequest)
            throws BaseException {
        final ActionLog userActionLog = mapper.convertValue(userActionLogRequest, ActionLog.class);
        return setResponseEntity(userActionLogServiceInterface.logUserActivityLog(userActionLog));
    }

    @PostMapping("/update-log")
    public ResponseEntity<CommonAdaptorResp<String>> updateLog(@RequestParam String activityId,
            @RequestParam String status,
            @RequestParam String statusDescription, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(
                userActionLogServiceInterface.updateUserActivityLog(activityId, status, statusDescription));
    }

    @PostMapping("/get-log")
    public ResponseEntity<CommonAdaptorResp<List<ActionLogData>>> getActionLog(
            @RequestBody TableFilterRequest actionLogRequest, HttpServletRequest httpServletRequest)
            throws BaseException {
        return setResponseEntity(userActionLogServiceInterface.getActionLog(actionLogRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonAdaptorResp<SingleActionLogResponse>> getActionLogById(@PathVariable Long id,
            HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(userActionLogServiceInterface.getActionLogById(id));
    }
}
