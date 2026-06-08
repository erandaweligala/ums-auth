package com.adl.et.telco.crm.usermanagerservice.controller;

import com.adl.et.telco.crm.usermanagerservice.dto.audit.ActionDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.audit.AuditSupportInterface;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequestMapping("/ums/meta")
@Tag(name = "Audit Support", description = "Audit Support")
public class AuditSupportController extends BaseController {

    @Autowired
    private AuditSupportInterface auditSupportInterface;

    //routeid-153 actionid-224
    @GetMapping("action/{id}")
    public ResponseEntity<CommonAdaptorResp<ActionDetails>> getActionDetails(@PathVariable(required = false) Long id, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(auditSupportInterface.getActionDetails(id));
    }

    //routeid-152 actionid-223
    @GetMapping("action")
    public ResponseEntity<CommonAdaptorResp<List<ActionDetails>>> getActions(@PathVariable(required = false) Long id, HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(auditSupportInterface.getActions());
    }

    //routeid-151 actionid-222
    @GetMapping("users")
    public ResponseEntity<CommonAdaptorResp<List<String>>> getAllUsers(HttpServletRequest httpServletRequest) throws BaseException {
        return setResponseEntity(auditSupportInterface.getAllUsers());
    }
}

