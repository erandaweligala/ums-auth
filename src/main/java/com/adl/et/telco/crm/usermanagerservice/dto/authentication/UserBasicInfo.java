package com.adl.et.telco.crm.usermanagerservice.dto.authentication;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserBasicInfo {
    private String userName;
    private String name;
    private String email;
    private String msisdn;
    private String role;
    private PermissionDTO permission;
    private String status;
    private String lastLogin;
    private String tid;
}

