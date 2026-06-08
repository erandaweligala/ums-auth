package com.adl.et.telco.crm.usermanagerservice.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Kafka payload sent from UMS to db-write-service for MySQL DR replication.
 *
 * <p>Field usage by eventType:
 * <pre>
 * INSERT                → tableName + columnValues
 * UPDATE                → tableName + columnValues + whereConditions
 * DELETE                → tableName + whereConditions
 * NATIVE_QUERY          → nativeQuery + queryParams  (no tableName)
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBWriteRequestMySQL {
    private String eventType;
    private String timestamp;
    private String userName;

    // Structured write fields
    private Map<String, Object> columnValues;
    private Map<String, Object> whereConditions;
    private String tableName;

    // Native query — for complex DML (JOIN updates, date-function queries)
    // SQL must use ? placeholders only — never interpolated values
    private String nativeQuery;
    private List<Object> queryParams;

    // Transactional child writes (e.g. permission + permission_to_actions)
    private List<DBWriteRequestMySQL> relatedWrites;

}
