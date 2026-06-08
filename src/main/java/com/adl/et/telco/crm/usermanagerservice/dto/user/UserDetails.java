package com.adl.et.telco.crm.usermanagerservice.dto.user;



import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails{
    private String lastLoginDateTime;
    private String mobileNumber;
    private String roleId;
    private String name;
    private String roleName;
    private String userId;
    private String username;
    private String email;
    private String status;
    private String userAccount;
    private String userType;
    private String defaultGroup;
    private String associatedUserGroup;
    private String createdBy;
    private String createdDate;
}

