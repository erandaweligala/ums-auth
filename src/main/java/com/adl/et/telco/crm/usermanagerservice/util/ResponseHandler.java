package com.adl.et.telco.crm.usermanagerservice.util;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.PageDetail;
import com.adl.et.telco.crm.usermanagerservice.dto.common.Result;
import org.springframework.stereotype.Component;

@Component
public class ResponseHandler {
    public <T> CommonAdaptorResp<T> respHandler(String code, String description){
        CommonAdaptorResp<T> resp = new CommonAdaptorResp<>();
        commonAdaptorRespCreator(resp,code,description);
        return resp;
    }

    public <T> CommonAdaptorResp<T> responseBuilder(String code, String description){
        CommonAdaptorResp<T> resp = new CommonAdaptorResp<>();
        Result result = new Result();
        result.setResultCode(code);
        result.setResultDescription(description);
        resp.setResult(result);
        return resp;
    }

    public <T> CommonAdaptorResp<T> responseBuilder( String code, String description,T object){
        CommonAdaptorResp<T> resp = new CommonAdaptorResp<>();
        Result result = new Result();
        result.setResultCode(code);
        result.setResultDescription(description);
        resp.setResult(result);
        resp.setResponseData(object);
        return resp;
    }
    public <T> CommonAdaptorResp<T> responseBuilder(String code, String description, T object, PageDetail pageDetail){
        CommonAdaptorResp<T> resp = new CommonAdaptorResp<>();
        Result result = new Result();
        result.setPageDetail(pageDetail);
        result.setResultCode(code);
        result.setResultDescription(description);
        resp.setResult(result);
        resp.setResponseData(object);
        return resp;
    }

    private void commonAdaptorRespCreator(CommonAdaptorResp commonAdaptorResp, String code, String description){
        Result result = new Result();
        result.setResultCode(code);
        result.setResultDescription(description);

    }
}

