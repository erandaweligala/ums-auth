package com.adl.et.telco.crm.usermanagerservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String clientIp = request.getRemoteAddr();

        String tenantId  = request.getHeader("tenantId");

        if(clientIp != null){
            MDC.put("Client-IP", clientIp);
        }

        if(tenantId != null){
            MDC.put("tenantId", tenantId);
        }else
            log.error("UMS_REQUEST_RECEIVED WITHOUT TENANT ID | REQUEST_METHOD : {} | REQUEST_URI {} ", request.getMethod(), request.getRequestURI());

        MDC.put("Message-ID", UUID.randomUUID().toString());
        log.info("UMS_REQUEST_RECEIVED | REQUEST_METHOD : {} | REQUEST_URI {} ", request.getMethod(), request.getRequestURI());

        chain.doFilter(request,response);
    }
}

