package com.adl.et.telco.crm.usermanagerservice.util.constants;

public enum StatusEnum {

    ACTIVE(1L);
    private Long value;

    StatusEnum(Long code ) {
        this.value = code;
    }

    public Long value() {
        return value;
    }
}

