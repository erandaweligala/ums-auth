package com.adl.et.telco.crm.usermanagerservice.dto.authentication;

import lombok.*;

import java.util.List;

@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AttributeDetails {
    private Long actionId;
    private List<AttributeItem> attributeList;
}

