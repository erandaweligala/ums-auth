package com.adl.et.telco.crm.usermanagerservice.dto.permission.menuandcomponents;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentsItem{
    private String componentId;
    private String componentName;
    private String description;
}

