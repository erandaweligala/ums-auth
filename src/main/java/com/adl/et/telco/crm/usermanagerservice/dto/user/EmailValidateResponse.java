package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailValidateResponse{
    private Boolean isValidUser;
    private ValidUserDetails userDetails;
}

