package com.adl.et.telco.crm.usermanagerservice.dto.azure;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AppRoleAssignmentsResponse{
    private String resourceDisplayName;
    private String resourceId;
    private String principalDisplayName;
    private String appRoleId;
    private String createdDateTime;
    private Object deletedDateTime;
    private String principalId;
    private String odataContext;
    private String id;
    private String principalType;

}
