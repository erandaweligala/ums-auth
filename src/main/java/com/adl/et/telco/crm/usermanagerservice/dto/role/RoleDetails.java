package com.adl.et.telco.crm.usermanagerservice.dto.role;

import lombok.*;

import java.util.List;
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleDetails{
    private String roleId;
    private List<PermissionsItem> permissions;
    private String roleName;
    private String description;
}
