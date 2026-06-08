package com.adl.et.telco.crm.usermanagerservice.dto.authentication;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionDTO {
    private List<Long> menuids;
    private List<ViewDTO> components;
}

