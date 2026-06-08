package com.adl.et.telco.crm.usermanagerservice.dto.role;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionsItem{
    private String permissionId;
    private String componentId;
    private String menuId;
    private String menuName;
    private String componentName;
    private String permissionDescription;
    private String permissionName;
}
