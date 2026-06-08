package com.adl.et.telco.crm.usermanagerservice.util.resultenum;

public enum ResponseCodeEnum {
    SUCCESSFUL("00","SUCCESSFUL"),
    USER_CREATION_SUCCESSFUL("00","USER CREATED SUCCESSFULLY"),
    USER_EDIT_SUCCESSFUL("00","USER EDIT SUCCESSFUL"),
    PERMISSION_CREATION_SUCCESSFUL("00","PERMISSION CREATED SUCCESSFULLY"),
    PERMISSION_EDIT_SUCCESSFUL("00","PERMISSION EDIT SUCCESSFUL"),
    ROLE_CREATION_SUCCESSFUL("00","ROLE CREATED SUCCESSFULLY"),
    ROLE_EDIT_SUCCESSFUL("00","ROLE EDIT SUCCESSFUL"),

    //Internal Exceptions
    EXCEPTION_ADAPTER_INTEGRATION_LAYER("97", "EXCEPTION IN ADAPTER INTEGRATION LAYER"),
    EXCEPTION_SERVICE_LAYER("96", "EXCEPTION IN SERVICE LAYER"),
    EXCEPTION_REPOSITORY_LAYER("98","EXCEPTION IN REPOSITORY LAYER"),
    INTERNAL_SERVER_ERROR("99", "INTERNAL SERVER ERROR"),
    EXCEPTION_ADVISER_CONTROLLER("95", "EXCEPTION IN LOGGING ADVISER - CONTROLLER"),
    EXCEPTION_ADVISER_SERVICE_LAYER("94", "EXCEPTION IN LOGGING ADVISER - SERVICE LAYER"),
    EXCEPTION_ADVISER_ADAPTER_INTEGRATION_LAYER("93", "EXCEPTION IN LOGGING ADVISER - ADAPTER INTEGRATION"),
    BAD_REQUEST("32","BAD_REQUEST"),
    EXCEPTION_HTTP_CLIENT("98", "EXCEPTION IN HTTP CLIENT"),
    HTTP_CLIENT_ERROR_FROM_AZURE_AD("73","Http ClientError Exception"),
    //not found
    INVALID_USER("32","INVALID USER"),
    APP_ROLE_ASSIGNMENT_FAILED ("33","APP ROLE ASSIGNMENT FAILED"),
    INVALID_ACTION("34", "Invalid Action. Action ID Not Found In Database"),
    EMAIL_NOT_FOUND("35", "Email Not Found."),
    USER_ALREADY_EXISTS("36","User Already Exists." ),
    NAME_ALREADY_EXISTS("37", "Name already Exists,  Please choose a different name"),
    INVALID_ACTION_ID("34", "Invalid Action. Action ID Not Found In Database"),
    DATA_NOT_FOUND("34", "Data not found"),
    EXCEPTION_EXTERNAL_LAYER("99", "Exception in External layer"),
    OPERATION_FAILED("38","Failed to Publish into Kafka");

    private String code;
    private String description;

    ResponseCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}

