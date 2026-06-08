package com.adl.et.telco.crm.usermanagerservice.util.advice;

import com.adl.et.telco.crm.usermanagerservice.util.constants.LoggingAdviceConstants;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Component
@Slf4j
@Aspect
public class LoggingAdvice {

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) && args(.., request) && !within(org.springdoc..*)")
    public Object logGetMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) && args(@RequestBody body,.., request) && !within(org.springdoc..*)")
    public Object logPostMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request, Object body) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.PatchMapping) && args(@RequestBody body,.., request) && !within(org.springdoc..*)")
    public Object logPatchMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request, Object body) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    private Object logRequest(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringTypeName();
        String methodName = methodSignature.getName();
        long start = System.currentTimeMillis();
        MDC.put(LoggingAdviceConstants.API_NAME, methodName);
        MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
        MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
        log.info(LoggingAdviceConstants.REQUEST_INITIATED, request.getMethod(), request.getRequestURI());
        log.debug(LoggingAdviceConstants.FULL_REQUEST, Arrays.toString(joinPoint.getArgs()));
        return logResponseAndHeader(joinPoint, start, className, methodName);
    }

    @SneakyThrows
    public Object logResponseAndHeader(ProceedingJoinPoint joinPoint, long start, String className, String methodName) {
        ObjectMapper mapper = new ObjectMapper();
        Object result = null;
        try {
            result = joinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            String response = mapper.writeValueAsString(result);
            JSONParser parser = new JSONParser();
            String resultCode = (String) ((JSONObject) ((JSONObject) ((JSONObject) parser.parse(response)).get("body"))
                    .get("result")).get("resultCode");
            String resultDescription = (String) ((JSONObject) ((JSONObject) ((JSONObject) parser.parse(response))
                    .get("body")).get("result")).get("resultDescription");

            Long statusCodeValue = (Long) ((JSONObject) parser.parse(response)).get("statusCodeValue");
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.REQUEST_TERMINATED, resultDescription, resultCode, statusCodeValue,
                    elapsedTime);
            log.debug(LoggingAdviceConstants.FULL_RESPONSE, response);
            return result;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_REQUEST_TERMINATED, ex.getMessage(), ex.getReason(),
                    ex.getResultCode(), ex.getHttpStatus(), displayStackStraceArray(ex.getStackTraceElements()),
                    elapsedTime);
            log.debug(LoggingAdviceConstants.EXCEPTION_STACKTRACE, Arrays.toString(ex.getStackTraceElements()));
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(),
                    ex.getStackTraceElements());
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_REQUEST_TERMINATED, ex.getMessage(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_CONTROLLER.description(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_CONTROLLER.code(), HttpStatus.INTERNAL_SERVER_ERROR,
                    displayStackStraceArray(ex.getStackTrace()), elapsedTime);
            log.debug(LoggingAdviceConstants.EXCEPTION_STACKTRACE, Arrays.toString(ex.getStackTrace()));
            throw new BaseException(ex.getMessage(), ResponseCodeEnum.EXCEPTION_ADVISER_CONTROLLER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_ADVISER_CONTROLLER.code(),
                    ex.getStackTrace());
        }
    }

    @SneakyThrows
    @Around("execution(* com.adl.et.telco.crm.usermanagerservice.serviceimpl..*(..)))")
    public Object logServiceInfo(ProceedingJoinPoint proceedingJoinPoint) {
        String className = null;
        String methodName = null;
        long start = System.currentTimeMillis();
        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            className = methodSignature.getDeclaringTypeName();
            methodName = methodSignature.getName();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            log.debug(LoggingAdviceConstants.SERVICE_INITIATED, Arrays.toString(proceedingJoinPoint.getArgs()));
            Object object = proceedingJoinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.SERVICE_TERMINATED_INFO, elapsedTime);
            String response = new ObjectMapper().writeValueAsString(object);
            log.debug(LoggingAdviceConstants.SERVICE_TERMINATED, response, elapsedTime);
            return object;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_SERVICE_TERMINATED, ex.getMessage(), ex.getReason(),
                    ex.getResultCode(), ex.getHttpStatus(), elapsedTime);
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(),
                    ex.getStackTraceElements());
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_SERVICE_TERMINATED, ex.getMessage(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_SERVICE_LAYER.description(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_SERVICE_LAYER.code(), HttpStatus.INTERNAL_SERVER_ERROR,
                    elapsedTime);
            throw new BaseException(ex.getMessage(), ResponseCodeEnum.EXCEPTION_ADVISER_SERVICE_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR, ResponseCodeEnum.EXCEPTION_ADVISER_SERVICE_LAYER.code(),
                    ex.getStackTrace());
        }
    }

    @SneakyThrows
    @Around("execution(* com.adl.et.telco.crm.usermanagerservice.client..*(..)))")
    public Object logClientInfo(ProceedingJoinPoint proceedingJoinPoint) {
        String className = null;
        String methodName = null;
        long start = System.currentTimeMillis();
        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            className = methodSignature.getDeclaringTypeName();
            methodName = methodSignature.getName();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            log.debug(LoggingAdviceConstants.HTTP_CLIENT_INITIATED, Arrays.toString(proceedingJoinPoint.getArgs()));
            Object object = proceedingJoinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.HTTP_CLIENT_TERMINATED_INFO, elapsedTime);
            String response = new ObjectMapper().writeValueAsString(object);
            log.debug(LoggingAdviceConstants.HTTP_CLIENT_TERMINATED, response, elapsedTime);
            return object;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_HTTP_CLIENT_TERMINATED, ex.getMessage(), ex.getReason(),
                    ex.getResultCode(), ex.getHttpStatus(), elapsedTime);
            throw new BaseException(ex.getMessage(), ex.getReason(), ex.getHttpStatus(), ex.getResultCode(),
                    ex.getStackTraceElements());
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_HTTP_CLIENT_TERMINATED, ex.getMessage(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_ADAPTER_INTEGRATION_LAYER.description(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_ADAPTER_INTEGRATION_LAYER.code(),
                    HttpStatus.INTERNAL_SERVER_ERROR, elapsedTime);
            throw new BaseException(ex.getMessage(),
                    ResponseCodeEnum.EXCEPTION_ADVISER_ADAPTER_INTEGRATION_LAYER.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_ADVISER_ADAPTER_INTEGRATION_LAYER.code(), ex.getStackTrace());
        }
    }

    public String displayStackStraceArray(StackTraceElement[] stackTraceElements) {
        StringBuilder stringBuilder = new StringBuilder();
        if (stackTraceElements != null) {
            for (StackTraceElement elem : stackTraceElements) {
                if (elem.getClassName().startsWith(LoggingAdviceConstants.PACKAGE_ROOT) && elem.getLineNumber() > 0) {
                    stringBuilder.append(elem.toString());
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }
}
