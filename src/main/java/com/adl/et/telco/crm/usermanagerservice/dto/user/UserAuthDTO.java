package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthDTO {
    private String code;
    private String tenant;
    private String clientId;
    private String redirectUri;
}

