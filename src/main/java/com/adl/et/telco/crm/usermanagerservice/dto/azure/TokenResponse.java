package com.adl.et.telco.crm.usermanagerservice.dto.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TokenResponse {
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("ext_expires_in")
    private int extExpiresIn;
    @JsonProperty("access_token")
    private String accessToken;

}

