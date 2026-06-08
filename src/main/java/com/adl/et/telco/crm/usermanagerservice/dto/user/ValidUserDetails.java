package com.adl.et.telco.crm.usermanagerservice.dto.user;

import lombok.*;

@Setter
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidUserDetails {

    private String mobileNumber;
    private String name;
    private String email;

}

