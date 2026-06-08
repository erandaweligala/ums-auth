package com.adl.et.telco.crm.usermanagerservice.dto.permission;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionView{
    private String permissionId;
    private String name;
    private String description;
    private String componentName;
    private String menuName;
    private Integer totalCount;
}
