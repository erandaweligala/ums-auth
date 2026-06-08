package com.adl.et.telco.crm.usermanagerservice.dto.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserDetails {
    @JsonProperty("@odata.context")
    private String dataContext;
    @JsonProperty("@odata.id")
    private String dataId;
    private String id;
    private String displayName;
    private String mobilePhone;
    private String department;
    private String officeLocation;
    private ManagerInfo manager;
}

