package com.adl.et.telco.crm.usermanagerservice.dto.common;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PageDetail{
    private int pageNumber;
    private int pageElementCount;
    private long totalRecords;
}
