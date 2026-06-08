package com.adl.et.telco.crm.usermanagerservice.dto.audit;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionDetails {
    private Long id;
    private String name;
    private String description;
}
