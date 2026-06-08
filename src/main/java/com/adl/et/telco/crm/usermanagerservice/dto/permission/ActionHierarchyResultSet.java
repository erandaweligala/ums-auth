package com.adl.et.telco.crm.usermanagerservice.dto.permission;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionHierarchyResultSet {
    private Long attributeId;
    private Integer isManinAction;
    private Long actionId;
    private String attributeName;
    private Long mainActionId;
    private String actionName;
}

