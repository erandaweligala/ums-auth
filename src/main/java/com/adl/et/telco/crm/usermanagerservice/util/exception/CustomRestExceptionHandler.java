package com.adl.et.telco.crm.usermanagerservice.util.exception;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public <T> ResponseEntity<CommonAdaptorResp<T>> customBaseExceptionHandler(BaseException ex) {
        CommonAdaptorResp<T> commonAdaptorResp = new CommonAdaptorResp<>();
        Result result = new Result();
        result.setResultCode(ex.getResultCode());
        result.setResultDescription(ex.getReason());
        commonAdaptorResp.setResult(result);
        return  ResponseEntity.status(ex.getHttpStatus()).body(commonAdaptorResp);
    }

    @ExceptionHandler(Exception.class)
    public <T> ResponseEntity<CommonAdaptorResp<T>> customExceptionHandler(Exception ex){
        CommonAdaptorResp<T> commonAdaptorResp = new CommonAdaptorResp<>();
        Result result = new Result();
        result.setResultCode(ResponseCodeEnum.INTERNAL_SERVER_ERROR.code());
        result.setResultDescription(ResponseCodeEnum.INTERNAL_SERVER_ERROR.description());
        commonAdaptorResp.setResult(result);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(commonAdaptorResp);
    }
}

