package com.adl.et.telco.crm.usermanagerservice.dto.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ManagerInfo {
    @JsonProperty("@odata.type")
    private String dataType;
    @JsonProperty("@odata.id")
    private String dataId;
    private String displayName;
    private String mail;
    private ManagerInfo manager;
}

