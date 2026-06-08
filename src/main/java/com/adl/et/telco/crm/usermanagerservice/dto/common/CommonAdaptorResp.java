package com.adl.et.telco.crm.usermanagerservice.dto.common;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommonAdaptorResp<T> {
    private Result result;
    private T responseData;
}

