package com.adl.et.telco.crm.usermanagerservice.dto.permission;



import lombok.*;

import java.util.List;
@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDetails{
    private String permissionId;
    private String description;
    private String menuId;
    private String menuName;
    private String componentId;
    private String componentName;
    private List<MainActionsItem> mainActions;
    private String permissionName;
}
