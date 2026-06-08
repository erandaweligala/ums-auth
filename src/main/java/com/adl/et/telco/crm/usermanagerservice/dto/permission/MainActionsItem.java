package com.adl.et.telco.crm.usermanagerservice.dto.permission;



import lombok.*;

import java.util.List;
@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MainActionsItem{
    private Boolean isSelected;
    private String actionId;
    private List<AttributesItem> attributes;
    private List<SubActionsItem> subActions;
    private String actionName;
}
