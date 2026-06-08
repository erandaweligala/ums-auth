package com.adl.et.telco.crm.usermanagerservice.repository.azurerepositories;

import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class AccessTokenRepository {
    @Autowired
    private RedisTemplate<String ,String > redisTemplate;
    public AccessTokenRepository(RedisTemplate<String,String > redisTemplate){
        this.redisTemplate = redisTemplate;
    }
    public void saveAccessToken(String accessToken,int expiryTime){
        redisTemplate.opsForValue().set(Constants.AZURE_TOKEN ,accessToken);
        redisTemplate.expire(Constants.AZURE_TOKEN, expiryTime, TimeUnit.SECONDS);
    }
    public String getAccessToken(){
        return redisTemplate.opsForValue().get(Constants.AZURE_TOKEN );
    }
}
