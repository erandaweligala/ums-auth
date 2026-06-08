package com.adl.et.telco.crm.usermanagerservice.serviceimpl.query;

import com.adl.et.telco.crm.usermanagerservice.dto.user.FilterValue;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QueryBuilder {

    Map<String, String> actionLogColumnMappings = Map.of(
            "id", "id",
            "activityId", "activity_id",
            "activity", "activity",
            "status", "status",
            "user", "user",
            "createdDateTime", "created_at",
            "actionId","action_id",
            "activityType","activity_type"
    );

    public StringBuilder buildActionLogFiltration(List<FilterValue> filters, StringBuilder baseSql, String tableName) {
        if (filters == null || filters.isEmpty()) {
            return baseSql;
        }

        for (FilterValue filter : filters) {
            String dbColumn = actionLogColumnMappings.getOrDefault(filter.getColumnName(), null);
            String operation = filter.getOperation();
            String[] values = filter.getValue();

            if (dbColumn == null || operation == null) continue;

            if (dbColumn.equalsIgnoreCase("created_at")) {
                handleDateFilter(baseSql, tableName, operation, dbColumn, values);

            } else {
                switch (operation.toLowerCase()) {
                    case "equal":
                        handleEqualOperation(baseSql, tableName, dbColumn, values);
                        break;
                    case "include":
                        handleIncludeOperation(baseSql, tableName, dbColumn, values);
                        break;
                    case "is empty":
                        handleEmptyOperation(baseSql, tableName, dbColumn);
                        break;
                    default:
                        // Unknown operation
                        break;
                }
            }
        }

        return baseSql;
    }

    private void handleDateFilter(StringBuilder baseSql, String tableName, String operation, String dbColumn, String[] values) {
        String normalizedOp = operation.trim().toLowerCase();
        baseSql.append(Constants.AND_WITH_SPACE).append(tableName).append(Constants.DOT).append(dbColumn);
        switch (normalizedOp) {
            case "last 30 minutes":
                baseSql.append(" >= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '30' MINUTE");
                break;
            case "last day":
                baseSql.append(" >= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' DAY");
                break;
            case "last week":
                baseSql.append(" >= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '7' DAY");
                break;
            case "last year":
                baseSql.append(" >= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' YEAR");
                break;
            case "between":
                if (values.length >= 2) {
                    baseSql.append(" BETWEEN CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_START)
                            .append(" AND CAST(FROM_TZ(TO_TIMESTAMP('").append(values[1]).append(Constants.TIME_QUERY_END);
                }
                break;
            case "include":
                if (values.length >= 1) {
                    baseSql.append(" BETWEEN CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_START)
                            .append(" AND CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_END);
                }
                break;
            case "greater than":
                if (values.length >= 1) {
                    baseSql.append(" > CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_END);
                }
                break;
            case "greater than or equal":
                if (values.length >= 1) {
                    baseSql.append(" >= CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_START);
                }
                break;
            case "less than":
                if (values.length >= 1) {
                    baseSql.append(" < CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_START);
                }
                break;
            case "less than or equal":
                if (values.length >= 1) {
                    baseSql.append(" <= CAST(FROM_TZ(TO_TIMESTAMP('").append(values[0]).append(Constants.TIME_QUERY_END);
                }
                break;
            default:
                // Optionally log or ignore unsupported operations
                break;
        }
    }

    private void handleEmptyOperation(StringBuilder baseSql, String tableName, String dbColumn) {
        baseSql.append(" AND (")
                .append(tableName)
                .append(Constants.DOT)
                .append(dbColumn)
                .append(" IS NULL OR ")
                .append(dbColumn)
                .append(" = '')");
    }

    private void handleIncludeOperation(StringBuilder baseSql, String tableName, String dbColumn, String[] values) {
        if (values != null && values.length > 0) {
            baseSql.append(" AND (");
            for (int i = 0; i < values.length; i++) {
                baseSql.append("LOWER(")
                        .append(tableName)
                        .append(Constants.DOT)
                        .append(dbColumn)
                        .append(") LIKE '%")
                        .append(values[i].toLowerCase())
                        .append("%'");
                if (i < values.length - 1) {
                    baseSql.append(" OR ");
                }
            }
            baseSql.append(")");
        }
    }

    private void handleEqualOperation(StringBuilder baseSql, String tableName, String dbColumn, String[] values) {
        if (values != null && values.length > 0) {
            baseSql.append(" AND LOWER(")
                    .append(tableName)
                    .append(Constants.DOT)
                    .append(dbColumn)
                    .append(") IN (")
                    .append(Arrays.stream(values)
                            .map(val -> "'" + val.toLowerCase() + "'")
                            .collect(Collectors.joining(", ")))
                    .append(")");
        }
    }
}

