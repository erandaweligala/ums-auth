package com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication;

import com.adl.et.telco.crm.usermanagerservice.client.KeyCloakClient;
import com.adl.et.telco.crm.usermanagerservice.dto.authentication.*;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import com.adl.et.telco.crm.usermanagerservice.dto.user.UserAuthDTO;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.*;
import com.adl.et.telco.crm.usermanagerservice.repository.AttributeRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.KeycloakRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.authentication.UserDetailsServiceInterface;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsService implements UserDetailsServiceInterface {

    private final CrmUserRepository crmUserRepository;
    private final AttributeRepository attributeRepository;
    private final ResponseHandler responseHandler;
    private final KeycloakRepository keycloakRepository;
    private final KeyCloakClient keyCloakClient;

    public CommonAdaptorResp<UserBasicInfo> getBasicInfoByUserName(String userName) throws BaseException {
        try {
            UserBasicInfo userBasicInfo = null;
            Result result;
            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            String uniqueID = UUID.randomUUID().toString();
            int updateLastLogin = crmUserRepository.updateCrmUserLastLogTimeAndTid(String.valueOf(timestamp), uniqueID, userName);
            if (updateLastLogin == 1) {
                Optional<Users> userOptional = crmUserRepository.getUserForAuthByUserName(userName);
                if (userOptional.isPresent()) {
                    Users users = userOptional.get();
                    Optional<Roles> roles = users.getRoles().stream().filter(e -> e.getTenants().getId() == Long.parseLong(MDC.get(Constants.TENANT_ID))).findFirst();
                    PermissionDTO permission = getUserPermission(users);
                    users.setLastLoginDatetime(String.valueOf(timestamp));
                    userBasicInfo = UserBasicInfo.builder().userName(users.getUserName()).role(roles.isPresent() ? roles.get().getName() : null).name(users.getName()).email(users.getEmail()).tid(users.getSessionId()).msisdn(users.getMobileNumber()).permission(permission).lastLogin(String.valueOf(users.getLastLoginDatetime())).status(users.getStatus().getName()).build();
                    result = Result.builder().resultCode(ResponseCodeEnum.SUCCESSFUL.code()).resultDescription(ResponseCodeEnum.SUCCESSFUL.description()).build();
                } else
                    throw new BaseException(ResponseCodeEnum.INVALID_USER.description(), ResponseCodeEnum.INVALID_USER.description(), HttpStatus.FORBIDDEN, ResponseCodeEnum.INVALID_USER.code(), null);
                return CommonAdaptorResp.<UserBasicInfo>builder().result(result).responseData(userBasicInfo).build();
            } else
                throw new BaseException(ResponseCodeEnum.EXCEPTION_REPOSITORY_LAYER.description(), ResponseCodeEnum.EXCEPTION_REPOSITORY_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_REPOSITORY_LAYER.code(), null);
        } catch (BaseException e) {
            log.error("UMS_EXCEPTION | getBasicInfoByUserName | Error: {}", e.getMessage());
            throw new BaseException(e.getMessage(), e.getReason(), e.getHttpStatus(), e.getResultCode(), e.getStackTraceElements());

        } catch (Exception e) {
            log.error("UMS_EXCEPTION | getBasicInfoByUserName | Error: {}", e.getMessage());
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<List<AttributeDetails>> getAttributeDetails() throws BaseException {
        try {
            List<AttributeDetails> responseList = new ArrayList<>();
            List<Attributes> attributesList = attributeRepository.getAttributesDetails(Long.parseLong(MDC.get(Constants.TENANT_ID)));
            if(attributesList!=null && !attributesList.isEmpty()) {
                Map<Actions, List<Attributes>> map = attributesList.stream().collect(Collectors.groupingBy(Attributes::getActions));
                for (Map.Entry<Actions, List<Attributes>> entryByAction : map.entrySet()) {
                    responseList.add(AttributeDetails.builder()
                            .actionId(entryByAction.getKey().getId())
                            .attributeList(entryByAction.getValue().stream()
                                    .map(e -> AttributeItem.builder().path(e.getPath()).id(e.getId()).build()).collect(Collectors.toList()))
                            .build());
                }
            }
            return CommonAdaptorResp.<List<AttributeDetails>>builder().result(Result.builder().resultCode(ResponseCodeEnum.SUCCESSFUL.code()).resultDescription(ResponseCodeEnum.SUCCESSFUL.description()).build()).responseData(responseList).build();
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    @Override
    public CommonAdaptorResp<String> getUsername(UserAuthDTO userAuthDTO) throws BaseException {
        log.debug("UMS_SERVICE_INITIATED | getUsername | clientId: {}, tenant: {}",
                userAuthDTO.getClientId(), userAuthDTO.getTenant());

        try {
            String username = "";
            Optional<String> clientSecret = keycloakRepository.findClientSecretByAppAndTenant(
                    userAuthDTO.getTenant(),
                    userAuthDTO.getClientId()
            );

            if (clientSecret.isPresent()) {
                CommonAdaptorResp<String> response = keyCloakClient.getKeyCloakUserDetails(
                        clientSecret.get(),
                        userAuthDTO
                );
                username = response.getResponseData();
            } else {
                log.warn("UMS_WARNING | getUsername | Client secret not found");
            }

            log.debug("UMS_SERVICE_TERMINATED | getUsername | Successfully fetched username");
            return CommonAdaptorResp.<String>builder()
                    .result(Result.builder()
                            .resultCode(ResponseCodeEnum.SUCCESSFUL.code())
                            .resultDescription(ResponseCodeEnum.SUCCESSFUL.description())
                            .build())
                    .responseData(username)
                    .build();

        } catch (Exception e) {
            log.error("UMS_EXCEPTION | getUsername | Error: {}", e.getMessage());
            throw new BaseException(
                    e.getMessage(),
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(),
                    e.getStackTrace()
            );
        }
    }

    private PermissionDTO getUserPermission(Users users) throws BaseException {
        try {
            Set<Long> menuIds = new HashSet<>();
            Set<ViewDTO> componentIds = new HashSet<>();

            Optional<Roles> role = users.getRoles().stream().filter(e -> e.getTenants().getId().equals(Long.parseLong(MDC.get(Constants.TENANT_ID)))).findFirst();
            if (role.isPresent()) {
                List<Permission> permissionList = role.get().getPermissionList();


                for (Permission permission : permissionList) {
                    menuIds.add(permission.getMenus().getId());
                    ViewDTO viewDTO = new ViewDTO();
                    viewDTO.setId(permission.getComponents().getId());
                    List<Actions> actionsList = permission.getActionsList().stream().filter(a -> a.getTenantsList().stream().anyMatch(t -> t.getId() == Long.parseLong(MDC.get(Constants.TENANT_ID)))).collect(Collectors.toList());
                    viewDTO.setActions(actionsList.stream().map(Actions::getId).collect(Collectors.toList()));
                    List<Attributes> attributesList = permission.getAttributesList().stream().filter(a -> a.getTenantsList().stream().anyMatch(t -> t.getId() == Long.parseLong(MDC.get(Constants.TENANT_ID)))).collect(Collectors.toList());
                    viewDTO.setAttributes(attributesList.stream().map(Attributes::getId).collect(Collectors.toList()));
                    List<Attributes> controllableAttributeList = attributeRepository.getControllableAttributes(actionsList.stream().map(Actions::getId).collect(Collectors.toList()), Long.parseLong(MDC.get(Constants.TENANT_ID)));
                    viewDTO.setControllableAttributes(controllableAttributeList.stream().map(Attributes::getId).collect(Collectors.toList()));
                    componentIds.add(viewDTO);
                }

                PermissionDTO permissionDTO = new PermissionDTO(new ArrayList<>(menuIds), new ArrayList<>(componentIds));
                return permissionDTO;
            }
            return null;
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }


}

