package com.adl.et.telco.crm.usermanagerservice.util;

import com.adl.et.telco.crm.usermanagerservice.dto.common.PageDetail;

public class PageUtils {
    private PageUtils() {
    }

    public static int getPageNumber(int limit, int offset){
        return (offset / limit) + 1;
    }

    public static PageDetail buildPageDetails(int limit, int offset, long totalRecords, int pageElementCount){
        return PageDetail.builder().pageNumber(getPageNumber(limit,offset)).totalRecords(totalRecords).pageElementCount(pageElementCount).build();
    }


}
