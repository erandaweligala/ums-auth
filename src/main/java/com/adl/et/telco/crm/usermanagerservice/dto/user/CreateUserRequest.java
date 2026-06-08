package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest{
    private String roleId;
    private String mobileNumber;
    private String name;
    private String email;
    private String userName;
    private String status;
    private String createdBy;
    private String userAccount;
    private String userType;
    private String defaultGroup;
    private String associatedUserGroup;
}

