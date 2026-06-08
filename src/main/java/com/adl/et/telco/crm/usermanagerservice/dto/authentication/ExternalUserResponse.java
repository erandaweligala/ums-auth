package com.adl.et.telco.crm.usermanagerservice.dto.authentication;

import lombok.*;

@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ExternalUserResponse {
    private String userName;
    private String password;
    private String mobileNumber;
    private Double statusId;
    private String lastLogin;

    public ExternalUserResponse(String password, String mobileNumber,  Double statusId,String lastLogin){
        this.password=password;
        this.mobileNumber=mobileNumber;
        this.lastLogin=lastLogin;
        this.statusId=statusId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getStatusId() {
        return (statusId != null) ? String.valueOf(Math.round(statusId)) : null;
    }


    public String getLastLogin() {
        return lastLogin;
    }
}

