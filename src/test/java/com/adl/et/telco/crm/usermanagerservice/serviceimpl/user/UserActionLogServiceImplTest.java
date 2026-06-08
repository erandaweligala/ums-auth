package com.adl.et.telco.crm.usermanagerservice.serviceimpl.user;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.ActionLogData;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.SingleActionLogResponse;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.ActionLog;
import com.adl.et.telco.crm.usermanagerservice.repository.UserActionLogRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.query.QueryBuilder;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserActionLogServiceImplTest {

    @InjectMocks
    private UserActionLogServiceImpl service;

    @Mock
    private UserActionLogRepository repository;
    @Mock
    private ResponseHandler responseHandler;
    @Mock
    private EntityManager entityManager;
    @Mock
    private QueryBuilder queryBuilder;
    @Mock
    private Query dataQuery;
    @Mock
    private Query countQuery;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(service, "format", "yyyy-MM-dd HH:mm:ss");
    }

    // ---------- logUserActivityLog ----------

    @Test
    void logUserActivityLog_success() throws BaseException {
        when(responseHandler.responseBuilder(any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<String> resp =
                service.logUserActivityLog(new ActionLog());

        assertThat(resp).isNotNull();
        verify(repository).save(any());
    }

    @Test
    void logUserActivityLog_exception() {
        doThrow(new RuntimeException("DB error"))
                .when(repository).save(any());

        assertThatThrownBy(() -> service.logUserActivityLog(new ActionLog()))
                .isInstanceOf(BaseException.class);
    }

    // ---------- updateUserActivityLog ----------

    @Test
    void updateUserActivityLog_success() throws BaseException {
        when(responseHandler.responseBuilder(any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<String> resp =
                service.updateUserActivityLog("ACT1", "SUCCESS", "OK");

        assertThat(resp).isNotNull();
        verify(repository).updateStatus(any(), any(), any());
    }

    @Test
    void updateUserActivityLog_exception() {
        doThrow(new RuntimeException("fail"))
                .when(repository).updateStatus(any(), any(), any());

        assertThatThrownBy(() ->
                service.updateUserActivityLog("ACT1", "FAIL", "ERR"))
                .isInstanceOf(BaseException.class);
    }

    // ---------- getActionLog ----------

    @Test
    void getActionLog_success() throws BaseException {
        TableFilterRequest req = TableFilterRequest.builder()
                .limit(10)
                .offset(0)
                .build();

        ActionLog log = new ActionLog();
        log.setId(1L);
        log.setActivityId("A1");
        log.setActivity("LOGIN");
        log.setStatus("SUCCESS");
        log.setUser("admin");
        log.setCreatedAt(Timestamp.from(Instant.now()).toLocalDateTime());

        when(queryBuilder.buildActionLogFiltration(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(entityManager.createNativeQuery(contains("SELECT *"), eq(ActionLog.class)))
                .thenReturn(dataQuery);
        when(entityManager.createNativeQuery(contains("COUNT")))
                .thenReturn(countQuery);

        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(log));
        when(countQuery.getSingleResult()).thenReturn(1L);

        when(responseHandler.responseBuilder(any(), any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<ActionLogData>> resp = service.getActionLog(req);

        assertThat(resp).isNotNull();
    }

    @Test
    void getActionLog_exception() {
        when(entityManager.createNativeQuery(anyString(), eq(ActionLog.class)))
                .thenThrow(new RuntimeException("SQL error"));

        TableFilterRequest req = TableFilterRequest.builder()
                .limit(10)
                .offset(0)
                .build();

        assertThatThrownBy(() -> service.getActionLog(req))
                .isInstanceOf(BaseException.class);
    }

    // ---------- getActionLogById ----------

    @Test
    void getActionLogById_success() throws BaseException {
        ActionLog log = new ActionLog();
        log.setId(1L);
        log.setActivityId("A1");
        log.setActivity("LOGIN");
        log.setStatus("SUCCESS");
        log.setCreatedAt(Timestamp.from(Instant.now()).toLocalDateTime());
        log.setUpdatedAt(Timestamp.from(Instant.now()).toLocalDateTime());

        when(repository.findById(1L))
                .thenReturn(Optional.of(log));
        when(responseHandler.responseBuilder(any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<SingleActionLogResponse> resp =
                service.getActionLogById(1L);

        assertThat(resp).isNotNull();
    }

    @Test
    void getActionLogById_notFound() throws BaseException {
        when(repository.findById(anyLong()))
                .thenReturn(Optional.empty());
        when(responseHandler.responseBuilder(any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<SingleActionLogResponse> resp =
                service.getActionLogById(99L);

        assertThat(resp).isNotNull();
    }

    @Test
    void getActionLogById_exception() {
        when(repository.findById(anyLong()))
                .thenThrow(new RuntimeException("DB"));

        assertThatThrownBy(() -> service.getActionLogById(1L))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void getActionLogById_withNullDates_shouldSucceed() throws BaseException {
        ActionLog log = new ActionLog();
        log.setId(2L);
        log.setActivityId("A2");
        log.setActivity("LOGOUT");
        log.setStatus("SUCCESS");
        log.setCreatedAt(null);
        log.setUpdatedAt(null);

        when(repository.findById(2L)).thenReturn(Optional.of(log));
        when(responseHandler.responseBuilder(any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<SingleActionLogResponse> resp = service.getActionLogById(2L);
        assertThat(resp).isNotNull();
    }

    @Test
    void getActionLog_withEmptyResult_shouldSucceed() throws BaseException {
        TableFilterRequest req = TableFilterRequest.builder()
                .limit(10)
                .offset(0)
                .build();

        when(queryBuilder.buildActionLogFiltration(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(entityManager.createNativeQuery(contains("SELECT *"), eq(ActionLog.class)))
                .thenReturn(dataQuery);
        when(entityManager.createNativeQuery(contains("COUNT")))
                .thenReturn(countQuery);

        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(Collections.emptyList());
        when(countQuery.getSingleResult()).thenReturn(0L);

        when(responseHandler.responseBuilder(any(), any(), any(), any()))
                .thenReturn(new CommonAdaptorResp<>());

        CommonAdaptorResp<List<ActionLogData>> resp = service.getActionLog(req);
        assertThat(resp).isNotNull();
    }

//    @Test
//    @DisplayName("Test convertOutputDate - Successful Conversion with Timezone Awareness")
//    void convertOutputDate_success() {
//        // 1. Use a specific Instant to avoid local system timezone interference
//        // 2026-02-09T10:00:00Z (UTC)
//        Instant instant = Instant.parse("2026-02-09T10:00:00Z");
//        Timestamp ts = Timestamp.from(instant);
//
//        // Accessing private method via Reflection
//        String result = ReflectionTestUtils.invokeMethod(
//                service, "convertOutputDate", ts);
//
//        // Asia/Colombo is UTC + 5:30.
//        // UTC 10:00:00 + 5:30 = 15:30:00 in Colombo
//        assertNotNull(result);
//        assertTrue(result.contains("2026-02-09"), "Should contain the date part");
//        assertTrue(result.contains("15:30:00"), "Should contain the Colombo converted time part (10:00 UTC + 5:30)");
//    }

//    @Test
//    @DisplayName("Test convertOutputDate - Fallback Branch (Catch Block)")
//    void convertOutputDate_fallbackBranch() {
//        // To reach 100% coverage, we trigger the catch block by mocking the Timestamp
//        // to throw an exception when toInstant() is called.
//        Timestamp mockTimestamp = mock(Timestamp.class);
//        when(mockTimestamp.toInstant()).thenThrow(new RuntimeException("Trigger Catch"));
//
//        // The catch block returns utcTimestamp.toString() if it's not null
//        when(mockTimestamp.toString()).thenReturn("fallback-timestamp-string");
//
//        String result = ReflectionTestUtils.invokeMethod(
//                service, "convertOutputDate", mockTimestamp);
//
//        assertEquals("fallback-timestamp-string", result);
//    }

}
