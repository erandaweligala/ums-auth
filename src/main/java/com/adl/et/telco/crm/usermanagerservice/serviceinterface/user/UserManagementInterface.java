package com.adl.et.telco.crm.usermanagerservice.serviceinterface.user;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.user.*;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;

import java.util.List;

public interface UserManagementInterface {
    CommonAdaptorResp<String> createUser(CreateUserRequest newUser) throws BaseException;

    CommonAdaptorResp<String> editUser(EditUserRequest user) throws BaseException;

    CommonAdaptorResp<List<MetaData>> getUserStatusMetaData() throws BaseException;

    CommonAdaptorResp<UserDetails> getUserDetails(Long userId) throws BaseException;

    CommonAdaptorResp<EmailValidateResponse> validateEmail(EmailValidationRequest emailValidationRequest) throws BaseException;

    CommonAdaptorResp<List<AllUserDetails>> getAllUsers(int limit, int offset, String userName, Long roleId, Long statusId) throws BaseException;

    CommonAdaptorResp<UserDetails> getUserDetailsByEmail(String email) throws BaseException;

    CommonAdaptorResp<String> expire(Integer days) throws BaseException;

    CommonAdaptorResp<List<MetaData>> getUserAccountMetaData() throws BaseException;

    CommonAdaptorResp<List<MetaData>> getUserTypeMetaData() throws BaseException;

    CommonAdaptorResp<List<MetaData>> getDefaultGroupMetaData() throws BaseException;

    CommonAdaptorResp<List<MetaData>> getUserGroupMetaData() throws BaseException;

    CommonAdaptorResp<List<AllUserDetails>> getAllFilteredUserList(TableFilterRequest tableFilterRequest) throws BaseException;
}

