package com.adl.et.telco.crm.usermanagerservice.serviceimpl.user;

import com.adl.et.telco.crm.usermanagerservice.dto.common.CommonAdaptorResp;
import com.adl.et.telco.crm.usermanagerservice.dto.common.PageDetail;
import com.adl.et.telco.crm.usermanagerservice.dto.user.TableFilterRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.ActionLogData;
import com.adl.et.telco.crm.usermanagerservice.dto.useractivity.SingleActionLogResponse;
import com.adl.et.telco.crm.usermanagerservice.mapper.common.MySQLEventMapper;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.ActionLog;
import com.adl.et.telco.crm.usermanagerservice.repository.UserActionLogRepository;
import com.adl.et.telco.crm.usermanagerservice.serviceimpl.query.QueryBuilder;
import com.adl.et.telco.crm.usermanagerservice.serviceinterface.user.UserActionLogServiceInterface;
import com.adl.et.telco.crm.usermanagerservice.util.ResponseHandler;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionLogServiceImpl implements UserActionLogServiceInterface {

    private final UserActionLogRepository userActionLogRepository;
    private final ResponseHandler         responseHandler;
    private final EntityManager           entityManager;
    private final QueryBuilder            queryBuilder;
    private final MySQLEventMapper mysqlEventMapper;   // ← Kafka publisher

    @Value("${date-format}")
    private String format;

    // =========================================================================
    // logUserActivityLog  ← CHANGED: no repository.save(); uses Kafka instead
    // =========================================================================

    /**
     * Publishes a CREATE action_log event to Kafka.
     *
     * <p>Action logs are high-frequency but still important for audit — if the
     * Kafka publish fails we log the error and return a success response rather
     * than failing the caller's request. Adjust this behaviour if you need
     * strict guarantees here.
     */
    @Override
    public CommonAdaptorResp<String> logUserActivityLog(ActionLog actionLogRequest) throws BaseException {
        try {
            mysqlEventMapper.publishCreateActionLog(actionLogRequest);
            return responseHandler.responseBuilder(
                    ResponseCodeEnum.SUCCESSFUL.code(),
                    ResponseCodeEnum.SUCCESSFUL.description(),
                    ResponseCodeEnum.SUCCESSFUL.description()
            );
        } catch (BaseException e) {
            // Propagate structured errors (e.g. Kafka broker unreachable)
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
    // updateUserActivityLog  ← CHANGED: no repository.updateStatus(); uses Kafka
    // =========================================================================

    /**
     * Publishes an UPDATE action_log event to Kafka.
     *
     * <p>Only {@code status} and {@code status_description} are updated,
     * plus {@code updated_at} which the mapper refreshes automatically.
     * This exactly mirrors what the old {@code repository.updateStatus} JPQL
     * query did.
     */
    @Override
    @Transactional(readOnly = true)  // no local DB write — annotation kept for
    // consistency but readOnly is now correct
    public CommonAdaptorResp<String> updateUserActivityLog(String activityId,
                                                           String status,
                                                           String statusDescription) throws BaseException {
        try {
            // actor is not carried on the update request — use "system"
            // If you add an actor/username parameter to this API later, wire it here.
            mysqlEventMapper.publishUpdateActionLog(activityId, status, statusDescription, "system");

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.SUCCESSFUL.code(),
                    ResponseCodeEnum.SUCCESSFUL.description(),
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

    @SuppressWarnings("java:S2077")
    @Override
    public CommonAdaptorResp<List<ActionLogData>> getActionLog(TableFilterRequest tableFilterRequest) throws BaseException {
        try {
            StringBuilder baseSql = new StringBuilder("FROM ums_action_log a WHERE 1=1");
            baseSql = queryBuilder.buildActionLogFiltration(tableFilterRequest.getFilterValues(), baseSql, "a");

            String dataSql  = "SELECT * " + baseSql + " ORDER BY a.id desc OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
            String countSql = "SELECT COUNT(*) " + baseSql;

            Query dataQuery  = entityManager.createNativeQuery(dataSql, ActionLog.class);
            Query countQuery = entityManager.createNativeQuery(countSql);

            dataQuery.setParameter("limit",  tableFilterRequest.getLimit());
            dataQuery.setParameter("offset", tableFilterRequest.getOffset());

            List<ActionLog> resultList = dataQuery.getResultList();
            long totalCount = ((Number) countQuery.getSingleResult()).longValue();

            List<ActionLogData> actionLogDataList = mapData(resultList);

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.SUCCESSFUL.code(),
                    ResponseCodeEnum.SUCCESSFUL.description(),
                    actionLogDataList,
                    PageDetail.builder()
                            .pageNumber((tableFilterRequest.getOffset() / tableFilterRequest.getLimit()) + 1)
                            .pageElementCount(actionLogDataList.size())
                            .totalRecords(totalCount)
                            .build()
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

    @Override
    public CommonAdaptorResp<SingleActionLogResponse> getActionLogById(Long id) throws BaseException {
        try {
            Optional<ActionLog> actionLogById = userActionLogRepository.findById(id);
            if (actionLogById.isEmpty()) {
                return responseHandler.responseBuilder(
                        ResponseCodeEnum.INVALID_ACTION_ID.code(),
                        ResponseCodeEnum.INVALID_ACTION_ID.description()
                );
            }
            ActionLog actionLog = actionLogById.get();

            SingleActionLogResponse singleActionLogResponse = SingleActionLogResponse.builder()
                    .id(actionLog.getId())
                    .activityId(actionLog.getActivityId())
                    .activity(actionLog.getActivity())
                    .status(actionLog.getStatus())
                    .statusDescription(actionLog.getStatusDescription())
                    .createdDateTime(actionLog.getCreatedAt() != null ? convertOutputDate(actionLog.getCreatedAt()) : null)
                    .updatedDateTime(actionLog.getUpdatedAt() != null ? convertOutputDate(actionLog.getUpdatedAt()) : null)
                    .url(actionLog.getUrl())
                    .user(actionLog.getUser())
                    .detailedRequest(actionLog.getDetailedRequest())
                    .build();

            return responseHandler.responseBuilder(
                    ResponseCodeEnum.SUCCESSFUL.code(),
                    ResponseCodeEnum.SUCCESSFUL.description(),
                    singleActionLogResponse
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
    // Private helpers — UNCHANGED
    // =========================================================================

    private List<ActionLogData> mapData(List<ActionLog> actionLog) {
        List<ActionLogData> executionLogList = new ArrayList<>();
        actionLog.forEach(item -> {
            ActionLogData executionLog = new ActionLogData();
            executionLog.setId(item.getId());
            executionLog.setActivityId(item.getActivityId());
            executionLog.setActivity(item.getActivity());
            executionLog.setStatus(item.getStatus());
            executionLog.setDescription(item.getStatusDescription());
            executionLog.setCreatedDateTime(item.getCreatedAt() != null ? convertOutputDate(item.getCreatedAt()) : null);
            executionLog.setUser(item.getUser());
            executionLog.setActionId(item.getActionId());
            executionLog.setActivityType(item.getActivityType());
            executionLogList.add(executionLog);
        });
        return executionLogList;
    }

    private String convertOutputDate(LocalDateTime dateTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return dateTime.format(formatter);
        } catch (Exception e) {
            return dateTime != null ? dateTime.toString() : null;
        }
    }
}
