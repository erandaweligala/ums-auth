package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailValidationRequest {
    private String email;
    private String userType;
}

