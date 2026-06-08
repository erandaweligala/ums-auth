package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EditUserRequest {
    private String userId;
    private String roleId;
    private String mobileNumber;
    private String name;
    private String status;
    private String userType;
    private String defaultGroup;
    private String associatedUserGroup;
    private String updatedBy;
}

