package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllUserDetails{
    private String userId;
    private String name;
    private String username;
    private String email;
    private String status;
    private String roleName;
    private String userAccount;
    private String mobileNumber;
    private String lastLoginTime;
    private String userType;
    private String defaultGroup;
    private String userGroup;
    private String createdBy;
    private String createdDate;
    private Integer totalCount;
}
