package com.adl.et.telco.crm.usermanagerservice.util.constants;

public class LoggingAdviceConstants {

    private LoggingAdviceConstants(){}

    public static final String API_NAME = "API-Name";
    public static final String CLASS_NAME = "Class-Name";
    public static final String METHOD_NAME = "Method-Name";
    public static final String PACKAGE_ROOT = "com.adl.et.telco.crm.securerequesthandler";
    public static final String REQUEST_INITIATED = "UMS_REQUEST_INITIATED | REQUEST_METHOD : {} | REQUEST_URI : {}";
    public static final String FULL_REQUEST = "UMS_FULL_REQUEST | CONTROLLER_REQUEST : {}";
    public static final String FULL_RESPONSE = "UMS_FULL_RESPONSE | CONTROLLER_RESPONSE : {}";
    public static final String REQUEST_TERMINATED = "UMS_REQUEST_TERMINATED | RESPONSE_MESSAGE : {} | RESPONSE_CODE : {} | HTTP_STATUS : {} | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_REQUEST_TERMINATED = "UMS_EXCEPTION | REQUEST_TERMINATED | ERROR_MESSAGE : {} | ERROR_REASON : {} | ERROR_CODE : {} | HTTP_STATUS : {} | ERROR_STACKTRACE : {} | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_STACKTRACE = "UMS_EXCEPTION | STACKTRACE : {}";
    public static final String SERVICE_INITIATED = "UMS_SERVICE_INITIATED | REQUEST_ARGS : {}";
    public static final String SERVICE_TERMINATED = "UMS_SERVICE_TERMINATED | RESPONSE_BODY : {} | RESPONSE_TIME : {} ms";
    public static final String SERVICE_TERMINATED_INFO = "UMS_SERVICE_TERMINATED | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_SERVICE_TERMINATED = "UMS_EXCEPTION | SERVICE_TERMINATED | ERROR_MESSAGE : {} | ERROR_REASON : {} | ERROR_CODE : {} | HTTP_STATUS : {} | RESPONSE_TIME : {} ms";
    public static final String HTTP_CLIENT_INITIATED = "UMS_HTTP_CLIENT_INITIATED | REQUEST_ARGS : {}";
    public static final String HTTP_CLIENT_TERMINATED = "UMS_HTTP_CLIENT_TERMINATED | RESPONSE_BODY : {} | RESPONSE_TIME : {} ms";
    public static final String HTTP_CLIENT_TERMINATED_INFO = "UMS_HTTP_CLIENT_TERMINATED | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_HTTP_CLIENT_TERMINATED = "UMS_EXCEPTION | HTTP_CLIENT_TERMINATED | ERROR_MESSAGE : {} | ERROR_REASON : {} | ERROR_CODE : {} | HTTP_STATUS : {} | RESPONSE_TIME : {} ms";
}

