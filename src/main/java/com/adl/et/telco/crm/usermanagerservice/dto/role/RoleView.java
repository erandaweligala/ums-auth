package com.adl.et.telco.crm.usermanagerservice.dto.role;

import lombok.*;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleView {
    private String roleId;
    private String name;
    private String description;
    private Integer totalCount;

}

