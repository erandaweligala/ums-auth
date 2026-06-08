package com.adl.et.telco.crm.usermanagerservice.dto.role;

import lombok.*;

import java.util.List;
@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateNewRole{
    private List<Long> permissionIdList;
    private String roleName;
    private String description;
    private String createdBy;
}
