package com.adl.et.telco.crm.usermanagerservice.serviceimpl.azure;

import com.adl.et.telco.crm.usermanagerservice.client.AzureClient;
import com.adl.et.telco.crm.usermanagerservice.dto.azure.TokenResponse;
import com.adl.et.telco.crm.usermanagerservice.repository.azurerepositories.AccessTokenRepository;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AzureAccessTokenService {

    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Autowired
    private AzureClient azureClient;

    public TokenResponse generateAccessToken() throws BaseException {
        try {
            TokenResponse tokenResponse = azureClient.getAzureToken();
            accessTokenRepository.saveAccessToken(tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());
            return tokenResponse;
        } catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

    public String getAccessToken()throws BaseException{
        try{
            String token=accessTokenRepository.getAccessToken();
            if(token == null)
                token=generateAccessToken().getAccessToken();
            return token;
        }catch (BaseException ex) {
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(), ex.getStackTraceElements());
        } catch (Exception e) {
            throw new BaseException(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(), HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(), e.getStackTrace());
        }
    }

}

