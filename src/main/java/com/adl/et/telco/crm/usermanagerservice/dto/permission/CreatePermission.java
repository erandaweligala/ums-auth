package com.adl.et.telco.crm.usermanagerservice.dto.permission;

import lombok.*;

import java.util.List;
@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePermission{
    private String componentId;
    private String createdBy;
    private String name;
    private String menuId;
    private String description;
    private List<Long> attributes;
    private List<Long> actions;
}
