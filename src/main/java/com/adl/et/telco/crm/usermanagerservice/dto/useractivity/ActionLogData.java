package com.adl.et.telco.crm.usermanagerservice.dto.useractivity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ActionLogData {
    private Long id;
    private String activityId;
    private String activity;
    private String description;
    private String status;
    private String user;
    private String createdDateTime;
    private Integer actionId;
    private String activityType;
}
