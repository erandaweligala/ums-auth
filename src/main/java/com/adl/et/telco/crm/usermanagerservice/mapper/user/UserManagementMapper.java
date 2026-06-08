package com.adl.et.telco.crm.usermanagerservice.mapper.user;

import com.adl.et.telco.crm.usermanagerservice.dto.user.UserDetails;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Roles;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Users;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Optional;

@Component
public class UserManagementMapper {
    public UserDetails mapUserDetails(Users user) throws BaseException {
        try {
            UserDetails userDetails = new UserDetails();
            userDetails.setUserId(String.valueOf(user.getId()));
            userDetails.setStatus(user.getStatus().getName());
            return getUserDetails(user, userDetails);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    private UserDetails getUserDetails(Users user, UserDetails userDetails) {
        userDetails.setName(user.getName());
        userDetails.setUsername(user.getUserName());
        userDetails.setEmail(user.getEmail());
        userDetails.setMobileNumber(user.getMobileNumber());
        userDetails.setLastLoginDateTime(getZoneDateTimeByString(user.getLastLoginDatetime()));
        userDetails.setUserAccount(user.getUserAccount());
        userDetails.setUserType(user.getUserType());
        userDetails.setDefaultGroup(user.getDefaultGroup());
        userDetails.setAssociatedUserGroup(user.getAssociatedUserGroup());
        userDetails.setCreatedBy(user.getCreatedBy() != null ? user.getCreatedBy().getName() : "SailPoint");
        userDetails.setCreatedDate(getZoneDateTimeByTimestamp(user.getCreatedDatetime()));

        Optional<Roles> role = user.getRoles().stream().filter(e -> e.getTenants().getId() == Long.parseLong(MDC.get(Constants.TENANT_ID))).findFirst();
        if (role.isPresent()) {
            userDetails.setRoleId(String.valueOf(role.get().getId()));
            userDetails.setRoleName(role.get().getName());
        }
        return userDetails;
    }

    private String getZoneDateTimeByString(String input) {
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern(Constants.DATE_TIME_PATTERN)
                    .optionalStart()
                    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 6, true)
                    .optionalEnd()
                    .toFormatter();

            LocalDateTime dateTime = LocalDateTime.parse(input, formatter);
            ZonedDateTime utcZoned = dateTime.atZone(ZoneId.of("UTC"));
            ZonedDateTime colomboZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Colombo"));

            return colomboZoned.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_PATTERN));
        } catch (Exception e) {
            return input;
        }
    }

    private String getZoneDateTimeByTimestamp(Timestamp dateTime) {
        ZoneId colomboZone = ZoneId.of("Asia/Colombo");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_TIME_PATTERN);
        return dateTime == null ?
                null : dateTime.toInstant().atZone(ZoneId.of("UTC")).withZoneSameInstant(colomboZone).format(formatter);
    }

    public UserDetails mapBasicUserDetails(Users user) throws BaseException {
        try {
            UserDetails userDetails = new UserDetails();
            userDetails.setUserId(String.valueOf(user.getId()));
            userDetails.setStatus(user.getStatus().getId().toString());
            return getUserDetails(user, userDetails);

        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }
}

