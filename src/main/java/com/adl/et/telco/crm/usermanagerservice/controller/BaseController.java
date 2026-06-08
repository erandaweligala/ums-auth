package com.adl.et.telco.crm.usermanagerservice.controller;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseController {

    public <T> ResponseEntity<CommonAdaptorResp<T>> setResponseEntity(CommonAdaptorResp<T> commonAdaptorResp) {
        String resultCode = commonAdaptorResp.getResult().getResultCode();
        switch (resultCode) {
            case "00":
                return ResponseEntity.status(HttpStatus.OK).body(commonAdaptorResp);
            case "01":
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(commonAdaptorResp);
            case "21":
            case "32":
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(commonAdaptorResp);
            case "30":
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(commonAdaptorResp);
            case "20":
            case "34":
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(commonAdaptorResp);
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonAdaptorResp);
        }
    }
}
