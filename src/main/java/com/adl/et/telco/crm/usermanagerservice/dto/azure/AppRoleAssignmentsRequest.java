package com.adl.et.telco.crm.usermanagerservice.dto.azure;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppRoleAssignmentsRequest{
    private String resourceId;
    private String appRoleId;
    private String principalId;
}

