package com.adl.et.telco.crm.usermanagerservice.dto.useractivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleActionLogResponse {
    private Long id;
    private String activityId;
    private String activity;
    private String createdDateTime;
    private String updatedDateTime;
    private String detailedRequest;
    private String status;
    private String statusDescription;
    private String url;
    private String user;
}
