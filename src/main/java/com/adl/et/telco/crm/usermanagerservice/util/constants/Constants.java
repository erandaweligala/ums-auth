package com.adl.et.telco.crm.usermanagerservice.util.constants;

import org.springframework.stereotype.Component;

@Component
public class Constants {
    public static final String TENANT_ID = "tenantId";
    public static final String CONTENTTYPE="Content-Type";
    public static final String CONTENTVALUE="application/x-www-form-urlencoded";
    public static final String CLIENT_ID="client_id";
    public static final String SCOPE="scope";
    public static final String CLIENT_SECRET="client_secret";
    public static final String GRANT_TYPE="grant_type";
    public static final String CLIENT_CREDENTIALS="client_credentials";
    public static final String APPLICATION_ID="applicationId";
    public static final String AZURE_TOKEN="AZURE_TOKEN";
    public static final String AUTHORIZATION="Authorization";
    public static final String BEARER= "Bearer ";
    public static final String CONSISTENCY_LEVEL="consistencylevel";
    public static final String EVENTUAL="eventual";
    public static final String EMAIL="email";
    public static final String AD_USER_ID = "user_id";
    public static final String APPLICATION_JSON = "application/json";
    public static final String FILTER_TYPE = "FilterType";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DOT = ".";
    public static final String AND_WITH_SPACE = " AND ";
    public static final String TIME_QUERY_START = " 00:00:00.000', '+05:30', '+00:00')";
    public static final String TIME_QUERY_END = " 23:59:59.999', '+05:30', '+00:00')";

    private Constants(){}

    public static final String WIDGET="widget";
}

