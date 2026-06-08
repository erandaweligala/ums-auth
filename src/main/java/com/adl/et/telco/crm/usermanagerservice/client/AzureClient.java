package com.adl.et.telco.crm.usermanagerservice.client;


import com.adl.et.telco.crm.usermanagerservice.dto.azure.AppRoleAssignmentsRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.AppRoleAssignmentsResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.TokenResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.UserDetails;
import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
public class AzureClient {
    @Value("${azure.ad.client-id}")
    private String azureClientId;
    @Value("${azure.ad.client-secret}")
    private String azureClientSecret;
    @Value("${azure.ad.application-id}")
    private String azureApplicationId;
    @Value("${azure.ad.scope}")
    private String azureScope;
    @Value("${azure.access.token.url}")
    private String accessTokenUrl;
    @Value("${azure.user-details.url}")
    private String azureUserDetailsUrl;
    @Value("${azure.app-role-assignment.url}")
    private String azureAppRoleAssignmentUrl;
    @Value("${azure.app-role.id}")
    private String azureAppRoleId;
    @Value("${azure.enterprise-app-obj.id}")
    private String azureEnterpriseAppObjectId;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ResponseHandler handler;


    public TokenResponse getAzureToken() throws BaseException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(Constants.CONTENTTYPE, Constants.CONTENTVALUE);
            MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
            bodyMap.add(Constants.GRANT_TYPE, Constants.CLIENT_CREDENTIALS);
            bodyMap.add(Constants.CLIENT_ID, azureClientId);
            bodyMap.add(Constants.SCOPE, azureScope);
            bodyMap.add(Constants.CLIENT_SECRET, azureClientSecret);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyMap, headers);

            Map<String, String> urlParams = new HashMap<>();
            urlParams.put(Constants.APPLICATION_ID, azureApplicationId);
            String url = UriComponentsBuilder.fromUriString(accessTokenUrl).buildAndExpand(urlParams).toString();

            return restTemplate.exchange(url, HttpMethod.POST, requestEntity, TokenResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.description(), e.getStatusCode(), ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.code(), e.getStackTrace());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.code(), e.getStackTrace());
        }
    }


    public CommonAdaptorResp<UserDetails> getAzureUserDetails(String email, String accessToken) throws BaseException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(Constants.CONSISTENCY_LEVEL, Constants.EVENTUAL);
            headers.add(Constants.AUTHORIZATION, Constants.BEARER + accessToken);

            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

            Map<String, String> urlParams = new HashMap<>();
            urlParams.put(Constants.EMAIL, email);
            String url = UriComponentsBuilder.fromUriString(azureUserDetailsUrl).buildAndExpand(urlParams).toString();

            ResponseEntity<UserDetails> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, UserDetails.class);
            return handler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.description(), e.getStatusCode(), ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.code(), e.getStackTrace());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.code(), e.getStackTrace());
        }
    }

    public CommonAdaptorResp<AppRoleAssignmentsResponse> grantSSOAccessForADUser(String userId, String accessToken) throws BaseException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(Constants.AUTHORIZATION, Constants.BEARER + accessToken);
            headers.add(HttpHeaders.CONTENT_TYPE,Constants.APPLICATION_JSON);
            headers.add(HttpHeaders.ACCEPT,Constants.APPLICATION_JSON);

            HttpEntity<AppRoleAssignmentsRequest> requestEntity = new HttpEntity<>(
                    AppRoleAssignmentsRequest.builder()
                            .resourceId(azureEnterpriseAppObjectId)
                            .principalId(userId)
                            .appRoleId(azureAppRoleId)
                            .build(), headers);

            Map<String, String> urlParams = new HashMap<>();
            urlParams.put(Constants.AD_USER_ID, userId);
            String url = UriComponentsBuilder.fromUriString(azureAppRoleAssignmentUrl).buildAndExpand(urlParams).toString();

            ResponseEntity<AppRoleAssignmentsResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, AppRoleAssignmentsResponse.class);
            return handler.responseBuilder(ResponseCodeEnum.SUCCESSFUL.code(), ResponseCodeEnum.SUCCESSFUL.description(), responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.description(), e.getStatusCode(), ResponseCodeEnum.HTTP_CLIENT_ERROR_FROM_AZURE_AD.code(), e.getStackTrace());
        } catch (Exception e) {
            throw new BaseException(e.getMessage(), ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_HTTP_CLIENT.code(), e.getStackTrace());
        }
    }
}
