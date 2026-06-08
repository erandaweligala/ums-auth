package com.adl.et.telco.crm.usermanagerservice.serviceimpl.authentication;

import com.adl.et.telco.crm.usermanagerservice.client.KeyCloakClient;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.repository.KeycloakRepository;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeyCloakManagerService {

    private final KeyCloakClient keyCloakClient;
    private final KeycloakRepository keycloakRepository;

    public Map<String, Object> getKeyCloakUserByEmail(String email, String tenantId, String clientId) throws BaseException {

        try {
            Optional<String> clientSecret = keycloakRepository.findClientSecretByAppAndTenant(tenantId, clientId);

            // get AccessToken
            String accessToken = keyCloakClient.getAdminAccessToken(clientId, clientSecret.get(), tenantId);

            // get user detail
            CommonAdaptorResp<Map<String, Object>> userDetail = keyCloakClient.getKeyCloakUserByEmail(email, accessToken, tenantId);

            return userDetail.getResponseData();
        } catch (Exception e) {
            throw new BaseException(
                    "Keycloak user fetching failed",
                    ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_EXTERNAL_LAYER.code(),
                    e.getStackTrace()
            );
        }
    }
}

