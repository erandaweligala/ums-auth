package com.adl.et.telco.crm.usermanagerservice.util.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class BaseException extends Exception{

    private final String reason;
    private final HttpStatusCode httpStatus;
    private final String resultCode;
    private final StackTraceElement[] stackTraceElements;

    public BaseException(String message, String reason, HttpStatusCode httpStatus, String resultCode, StackTraceElement[] stackTraceElements){
        super(message);
        this.stackTraceElements = stackTraceElements;
        this.httpStatus = httpStatus;
        this.resultCode = resultCode;
        this.reason = reason;
    }
}
