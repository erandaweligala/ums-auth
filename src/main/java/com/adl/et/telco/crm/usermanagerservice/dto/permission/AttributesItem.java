package com.adl.et.telco.crm.usermanagerservice.dto.permission;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttributesItem{
    private String attributeId;
    private Boolean isSelected;
    private String attributeName;
}
