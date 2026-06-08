package com.adl.et.telco.crm.usermanagerservice.serviceimpl.user;

import com.adl.et.telco.crm.usermanagerservice.client.AzureClient;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.AppRoleAssignmentsResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.user.*;
import com.adl.et.telco.crm.usermanagerservice.filter.FilterValueExtractor;
import com.adl.et.telco.crm.usermanagerservice.mapper.common.MySQLEventMapper;
import com.adl.et.telco.crm.usermanagerservice.mapper.user.UserManagementMapper;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Roles;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Status;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Users;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.DefaultGroupRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.RoleRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.StatusRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.UserAccountRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.UserGroupRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.UserTypeRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication.KeyCloakManagerService;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.azure.AzureAccessTokenService;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.user.UserManagementInterface;
import com.adl.et.telco.crm.usermanagerservice.util.PageUtils;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementInterface {

    private final CrmUserRepository       crmUserRepository;
    private final RoleRepository          roleRepository;
    private final StatusRepository        statusRepository;
    private final UserAccountRepository   userAccountRepository;
    private final UserTypeRepository      userTypeRepository;
    private final DefaultGroupRepository  defaultGroupRepository;
    private final UserGroupRepository     userGroupRepository;
    private final ResponseHandler         responseHandler;
    private final UserManagementMapper    userManagementMapper;
    private final AzureAccessTokenService azureAccessTokenService;
    private final AzureClient             azureClient;
    private final FilterValueExtractor    filterValueExtractor;
    private final KeyCloakManagerService  keyCloakManagerService;
    private final MySQLEventMapper        mysqlEventMapper;   // ← Kafka publisher

    @Value("${app-role-assignment-required}")
    private boolean adRoleAssignmentRequired;

    @Value("${is-key-cloak-enabled}")
    private boolean isKeyCloakEnabled;

    @Value("${keycloak.tenant-id}")
    private String tenantId;

    @Value("${keycloak.client-id}")
    private String clientId;

    private final List<Long> validStatusForCreateUser = Collections.singletonList(3L);

    // =========================================================================
    // createUser  ← CHANGED: no crmUserRepository.save(); uses Kafka instead
    // =========================================================================

    @Override
    public CommonAdaptorResp<String> createUser(CreateUserRequest newUser) throws BaseException {
        try {
            String normalizedEmail = newUser.getEmail().trim().toLowerCase();

            // Check email across ALL statuses
            boolean emailExists = crmUserRepository.existsByEmailAndTenantId(
                    normalizedEmail,
                    Long.parseLong(MDC.get(Constants.TENANT_ID))
            ) > 0L;
            if (emailExists) {
                throw new BaseException(
                        ResponseCodeEnum.USER_ALREADY_EXISTS.description(),
                        ResponseCodeEnum.USER_ALREADY_EXISTS.description(),
                        HttpStatus.CONFLICT,
                        ResponseCodeEnum.USER_ALREADY_EXISTS.code(),
                        null
                );
            }

            // Role and status validation — read-only, just to validate input
            List<Roles> roles = roleRepository.findAllById(
                    Stream.of(Long.parseLong(newUser.getRoleId())).collect(Collectors.toList())
            );
            Optional<Status> userStatus = statusRepository.findById(Long.parseLong(newUser.getStatus()));

            if (roles.isEmpty() || userStatus.isEmpty()) {
                throw new BaseException(
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.BAD_REQUEST.code(),
                        null
                );
            }

            // Fire-and-forget Azure role assignment (unchanged)
            if (adRoleAssignmentRequired) {
                triggerAzureRoleAssignmentAsync(normalizedEmail);
            }

            // ── Kafka publish (replaces crmUserRepository.save) ──────────
            String actor = normalizedEmail; // best available identity at create time
            mysqlEventMapper.publishCreateUser(newUser, actor, normalizedEmail);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.USER_CREATION_SUCCESSFUL.code(),
                    ResponseCodeEnum.USER_CREATION_SUCCESSFUL.description()
            );

        } catch (BaseException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(
                    ResponseCodeEnum.USER_ALREADY_EXISTS.description(),
                    ResponseCodeEnum.USER_ALREADY_EXISTS.description(),
                    HttpStatus.CONFLICT,
                    ResponseCodeEnum.USER_ALREADY_EXISTS.code(),
                    null
            );
        } catch (Exception e) {
            throw new BaseException(
                    e.getMessage(),
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(),
                    e.getStackTrace()
            );
        }
    }

    // =========================================================================
    // editUser  ← CHANGED: no crmUserRepository.save(); uses Kafka instead
    // =========================================================================

    @Override
    public CommonAdaptorResp<String> editUser(EditUserRequest user) throws BaseException {
        try {
            // Validate user and status exist (read-only)
            Optional<Users> existingUser = crmUserRepository.findById(Long.parseLong(user.getUserId()));
            List<Roles>     roles        = roleRepository.findAllById(
                    Stream.of(Long.parseLong(user.getRoleId())).collect(Collectors.toList())
            );
            Optional<Status> userStatus = statusRepository.findById(Long.parseLong(user.getStatus()));

            if (existingUser.isEmpty() || userStatus.isEmpty() || roles.isEmpty()) {
                throw new BaseException(
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        ResponseCodeEnum.BAD_REQUEST.description(),
                        HttpStatus.BAD_REQUEST,
                        ResponseCodeEnum.BAD_REQUEST.code(),
                        null
                );
            }

            // ── Kafka publish (replaces crmUserRepository.save) ──────────
            String actor = existingUser.get().getUserName();
            mysqlEventMapper.publishEditUser(user, actor);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.USER_EDIT_SUCCESSFUL.code(),
                    ResponseCodeEnum.USER_EDIT_SUCCESSFUL.description()
            );

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    e.getMessage(),
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(),
                    e.getStackTrace()
            );
        }
    }

    // =========================================================================
    // expire  ← CHANGED: no crmUserRepository.expireUser(); uses Kafka instead
    // =========================================================================

    @Override
    @Transactional(readOnly = true)  // read-only: no local DB writes
    public CommonAdaptorResp<String> expire(Integer days) throws BaseException {
        try {
            Long currentTenantId = Long.parseLong(MDC.get(Constants.TENANT_ID));
            // actor is a system operation; use a fixed system identity
            mysqlEventMapper.publishExpireUsers(days, currentTenantId, "system");

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.SUCCESSFUL.code(),
                    ResponseCodeEnum.SUCCESSFUL.description()
            );
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(
                    e.getMessage(),
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(),
                    e.getStackTrace()
            );
        }
    }

    // =========================================================================
    // Read-only methods — UNCHANGED
    // =========================================================================

    @Override
    public CommonAdaptorResp<List<MetaData>> getUserStatusMetaData() throws BaseException {
        try {
            List<MetaData> userStatus = statusRepository.getUserStatus();
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), userStatus);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<UserDetails> getUserDetails(Long userId) throws BaseException {
        try {
            Optional<Users> user = crmUserRepository.findById(userId);
            if (user.isPresent()) {
                UserDetails mappedUserDetails = userManagementMapper.mapUserDetails(user.get());
                return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), mappedUserDetails);
            }
            throw new BaseException(ResponseCodeEnum.BAD_REQUEST.description(), ResponseCodeEnum.BAD_REQUEST.description(), HttpStatus.NOT_FOUND, ResponseCodeEnum.BAD_REQUEST.code(), null);
        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<EmailValidateResponse> validateEmail(EmailValidationRequest emailValidationRequest) throws BaseException {
        try {
            boolean adValidation = false;
            boolean dbValidation = false;
            com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails userDetails = null;

            if (emailValidationRequest == null || emailValidationRequest.getEmail() == null || emailValidationRequest.getEmail().isEmpty()
                    || emailValidationRequest.getUserType() == null || emailValidationRequest.getUserType().isEmpty()) {
                throw new BaseException(ResponseCodeEnum.BAD_REQUEST.description(), ResponseCodeEnum.BAD_REQUEST.description(), HttpStatus.BAD_REQUEST, ResponseCodeEnum.BAD_REQUEST.code(), null);
            }
            String email = emailValidationRequest.getEmail().trim();

            if (emailValidationRequest.getUserType().equalsIgnoreCase("Azure")) {
                try {
                    String accessToken = azureAccessTokenService.getAccessToken();
                    userDetails = azureClient.getAzureUserDetails(email, accessToken).getResponseData();
                    adValidation = true;
                } catch (BaseException ex) {
                    if (!HttpStatus.NOT_FOUND.equals(ex.getHttpStatus())) {
                        log.error("UMS | Azure User Details Fetching Failed | {}", ex.getReason());
                    }
                    log.warn("UMS | User not found in Azure | {}", email);
                }
            } else if (emailValidationRequest.getUserType().equalsIgnoreCase("KeyCloak")) {
                if (isKeyCloakEnabled) {
                    userDetails = validateUserByKeyCloak(email);
                    if (userDetails != null) {
                        adValidation = true;
                    } else {
                        log.warn("UMS | User not found in KeyCloak | {}", email);
                    }
                }
            } else {
                throw new BaseException(ResponseCodeEnum.BAD_REQUEST.description(), ResponseCodeEnum.BAD_REQUEST.description(), HttpStatus.BAD_REQUEST, ResponseCodeEnum.BAD_REQUEST.code(), null);
            }

            if (adValidation) {
                int x = crmUserRepository.isExistingUser(email, validStatusForCreateUser, Long.parseLong(MDC.get(Constants.TENANT_ID)));
                dbValidation = x <= 0;
            }

            boolean userValidity = adValidation && dbValidation;
            ValidUserDetails validUserDetails = new ValidUserDetails();

            if (userValidity) {
                validUserDetails.setName(userDetails.getDisplayName());
                validUserDetails.setMobileNumber(userDetails.getMobilePhone());
                validUserDetails.setEmail(email);
            }

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.SUCCESSFUL.code(),
                    !adValidation ? ResponseCodeEnum.EMAIL_NOT_FOUND.description()
                            : !dbValidation ? ResponseCodeEnum.USER_ALREADY_EXISTS.description()
                            : ResponseCodeEnum.SUCCESSFUL.description(),
                    EmailValidateResponse.builder().isValidUser(userValidity).userDetails(validUserDetails).build()
            );
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<AllUserDetails>> getAllUsers(int limit, int offset, String userName, Long roleId, Long statusId) throws BaseException {
        try {
            List<AllUserDetails> allUserDetailsList = crmUserRepository.getAllUsers(limit, offset, userName, roleId, statusId, Long.parseLong(MDC.get(Constants.TENANT_ID)));
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), allUserDetailsList, PageUtils.buildPageDetails(limit, offset, !allUserDetailsList.isEmpty() ? allUserDetailsList.get(0).getTotalCount() : 0, allUserDetailsList.size()));
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<UserDetails> getUserDetailsByEmail(String email) throws BaseException {
        try {
            Optional<Users> user = crmUserRepository.findByEmailAndTenantId(email, Long.parseLong(MDC.get(Constants.TENANT_ID)));
            if (user.isPresent()) {
                return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), userManagementMapper.mapBasicUserDetails(user.get()));
            }
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), null);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<MetaData>> getUserAccountMetaData() throws BaseException {
        try {
            List<MetaData> metaDataList = userAccountRepository.getUserAccountMetaData();
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), metaDataList);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<MetaData>> getUserTypeMetaData() throws BaseException {
        try {
            List<MetaData> metaDataList = userTypeRepository.getUserTypeMetaData();
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), metaDataList);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<MetaData>> getDefaultGroupMetaData() throws BaseException {
        try {
            List<MetaData> metaDataList = defaultGroupRepository.getDefaultGroupMetaData();
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), metaDataList);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<MetaData>> getUserGroupMetaData() throws BaseException {
        try {
            List<MetaData> metaDataList = userGroupRepository.getUserGroupMetaData();
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), metaDataList);
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<AllUserDetails>> getAllFilteredUserList(TableFilterRequest tableFilterRequest) throws BaseException {
        try {
            Map<String, String> filterDataMap = filterValueExtractor.extractFilterValues(tableFilterRequest.getFilterValues());
            List<String> statusList = filterValueExtractor.findStatusList(tableFilterRequest.getFilterValues());
            List<AllUserDetails> allUserDetailsList = crmUserRepository.getAllFilteredUser(
                    filterDataMap.get("userId"),          filterDataMap.get("userId" + Constants.FILTER_TYPE),
                    filterDataMap.get("name"),            filterDataMap.get("name" + Constants.FILTER_TYPE),
                    filterDataMap.get("email"),           filterDataMap.get("email" + Constants.FILTER_TYPE),
                    statusList,
                    filterDataMap.get("roleName"),        filterDataMap.get("roleName" + Constants.FILTER_TYPE),
                    filterDataMap.get("userAccount"),     filterDataMap.get("userAccount" + Constants.FILTER_TYPE),
                    filterDataMap.get("mobileNumber"),    filterDataMap.get("mobileNumber" + Constants.FILTER_TYPE),
                    filterDataMap.get("lastLoginTime"),   filterDataMap.get("lastLoginTime" + Constants.FILTER_TYPE),
                    filterDataMap.get("userType"),        filterDataMap.get("userType" + Constants.FILTER_TYPE),
                    filterDataMap.get("defaultGroup"),    filterDataMap.get("defaultGroup" + Constants.FILTER_TYPE),
                    filterDataMap.get("userGroup"),       filterDataMap.get("userGroup" + Constants.FILTER_TYPE),
                    filterDataMap.get("createdDate"),     filterDataMap.get("createdDate" + Constants.FILTER_TYPE),
                    tableFilterRequest.getLimit(), tableFilterRequest.getOffset(),
                    Long.parseLong(MDC.get(Constants.TENANT_ID)));
            return responseHandler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), allUserDetailsList,
                    PageUtils.buildPageDetails(tableFilterRequest.getLimit(), tableFilterRequest.getOffset(),
                            !allUserDetailsList.isEmpty() ? allUserDetailsList.get(0).getTotalCount() : 0, allUserDetailsList.size()));
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    // =========================================================================
    // Private helpers — UNCHANGED
    // =========================================================================

    private void triggerAzureRoleAssignmentAsync(String email) {
        log.debug("UMS_INFO | createUser | Starting Azure role assignment");
        CompletableFuture.runAsync(() -> {
            try {
                String accessToken = azureAccessTokenService.generateAccessToken().getAccessToken();
                com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails userDetails =
                        azureClient.getAzureUserDetails(email, accessToken).getResponseData();
                AppRoleAssignmentsResponse assignmentsResponse = null;
                if (userDetails != null && userDetails.getId() != null) {
                    assignmentsResponse = azureClient.grantSSOAccessForADUser(userDetails.getId(), accessToken).getResponseData();
                } else {
                    throw new BaseException(ResponseCodeEnum.INVALID_USER.description(), ResponseCodeEnum.INVALID_USER.description(), HttpStatus.BAD_REQUEST, ResponseCodeEnum.INVALID_USER.code(), null);
                }
                if (assignmentsResponse == null || assignmentsResponse.getId() == null) {
                    throw new BaseException(ResponseCodeEnum.APP_ROLE_ASSIGNMENT_FAILED.description(), ResponseCodeEnum.APP_ROLE_ASSIGNMENT_FAILED.description(), HttpStatus.BAD_REQUEST, ResponseCodeEnum.APP_ROLE_ASSIGNMENT_FAILED.code(), null);
                }
            } catch (BaseException e) {
                log.error("UMS_EXCEPTION | createUser | Azure role assignment failed: {}", e.getReason());
            }
        });
    }

    private com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails validateUserByKeyCloak(String email) {
        com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails userDetails = null;
        try {
            Map<String, Object> keyCloakUserDetails = keyCloakManagerService.getKeyCloakUserByEmail(email, tenantId, clientId);
            String displayName = keyCloakUserDetails.get("firstName") + " " + keyCloakUserDetails.get("lastName");
            userDetails = new com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails();
            userDetails.setDisplayName(displayName);
        } catch (Exception e) {
            log.warn("UMS | User not found in KeyCloak or Error while fetching | {}", e.getMessage());
        }
        return userDetails;
    }
}