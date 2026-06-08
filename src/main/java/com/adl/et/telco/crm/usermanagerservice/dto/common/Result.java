package com.adl.et.telco.crm.usermanagerservice.dto.common;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Result {
    private String resultCode;
    private String resultDescription;
    private PageDetail pageDetail;
}

