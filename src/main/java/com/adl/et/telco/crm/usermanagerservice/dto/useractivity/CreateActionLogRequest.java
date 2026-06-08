package com.adl.et.telco.crm.usermanagerservice.dto.useractivity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateActionLogRequest {
    private String activity;
    private String activityId;
    private String detailedRequest;
    private String url;
    private String status;
    private String statusDescription;
    private String user;
}
