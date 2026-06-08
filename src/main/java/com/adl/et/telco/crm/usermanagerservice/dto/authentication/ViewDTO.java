package com.adl.et.telco.crm.usermanagerservice.dto.authentication;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ViewDTO {
    private Long id;
    private List<Long> actions;
    private List<Long> attributes;
    private List<Long> controllableAttributes;
}

