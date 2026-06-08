package com.adl.et.telco.crm.usermanagerservice.serviceimpl.query;

import com.adl.et.telco.crm.usermanagerservice.dto.user.FilterValue;
import com.adl.et.telco.crm.usermanagerservice.util.constants.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryBuilderTest {

    private QueryBuilder queryBuilder;
    private static final String TABLE = "action_log";

    @BeforeEach
    void setUp() {
        queryBuilder = new QueryBuilder();
    }

    @Test
    void shouldReturnBaseSql_whenFiltersNull() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        StringBuilder result = queryBuilder.buildActionLogFiltration(null, sql, TABLE);

        assertThat(result.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldReturnBaseSql_whenFiltersEmpty() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        StringBuilder result = queryBuilder.buildActionLogFiltration(List.of(), sql, TABLE);

        assertThat(result.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldIgnoreUnknownColumn() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("unknown", "equal", new String[]{"x"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldIgnoreNullOperation() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("status", null, new String[]{"active"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldHandleEqualOperation() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("status", "equal", new String[]{"ACTIVE", "INACTIVE"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString())
                .contains("LOWER(action_log.status) IN ('active', 'inactive')");
    }

    @Test
    void shouldHandleIncludeOperation() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("activity", "include", new String[]{"LOGIN", "LOGOUT"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString())
                .contains("LOWER(action_log.activity) LIKE '%login%'")
                .contains("OR")
                .contains("LOWER(action_log.activity) LIKE '%logout%'");
    }

    @Test
    void shouldHandleIsEmptyOperation() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("user", "is empty", null);

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString())
                .contains("action_log.user IS NULL")
                .contains("OR user = ''");
    }

    // -------- DATE FILTERS --------

    @Test
    void shouldHandleLast30Minutes() {
        assertDateFilter("last 30 minutes", ">= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '30' MINUTE");
    }

    @Test
    void shouldHandleLastDay() {
        assertDateFilter("last day", ">= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' DAY");
    }

    @Test
    void shouldHandleLastWeek() {
        assertDateFilter("last week", ">= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '7' DAY");
    }

    @Test
    void shouldHandleLastYear() {
        assertDateFilter("last year", ">= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' YEAR");
    }

    @Test
    void shouldHandleBetweenDates() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue(
                "createdDateTime",
                "between",
                new String[]{"2024-01-01", "2024-01-31"}
        );

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString())
                .contains("BETWEEN CAST(FROM_TZ(TO_TIMESTAMP('2024-01-01" + Constants.TIME_QUERY_START)
                .contains("AND CAST(FROM_TZ(TO_TIMESTAMP('2024-01-31" + Constants.TIME_QUERY_END);
    }

    @Test
    void shouldHandleIncludeDate() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue(
                "createdDateTime",
                "include",
                new String[]{"2024-01-01"}
        );

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString())
                .contains("BETWEEN CAST(FROM_TZ(TO_TIMESTAMP('2024-01-01" + Constants.TIME_QUERY_START);
    }

    @Test
    void shouldHandleGreaterThanDate() {
        assertDateFilter("greater than", ">");
    }

    @Test
    void shouldHandleGreaterThanOrEqualDate() {
        assertDateFilter("greater than or equal", ">=");
    }

    @Test
    void shouldHandleLessThanDate() {
        assertDateFilter("less than", "<");
    }

    @Test
    void shouldHandleLessThanOrEqualDate() {
        assertDateFilter("less than or equal", "<=");
    }

    @Test
    void shouldIgnoreUnsupportedOperation() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("status", "unknown-op", new String[]{"x"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldIgnoreUnknownDateOperation() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("createdDateTime", "unknown-date-op", new String[]{"2024-01-01"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        // The column reference is still appended, but no operation SQL
        assertThat(sql.toString()).contains("created_at");
    }

    @Test
    void shouldHandleBetweenDate_withInsufficientValues() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("createdDateTime", "between", new String[]{"2024-01-01"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        // Only column reference is appended, no BETWEEN clause
        assertThat(sql.toString()).contains("created_at");
        assertThat(sql.toString()).doesNotContain("BETWEEN");
    }

    @Test
    void shouldHandleEqualOperation_withNullValues() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("status", "equal", null);

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldHandleIncludeOperation_withNullValues() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("activity", "include", null);

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).isEqualTo("SELECT * FROM action_log");
    }

    @Test
    void shouldHandleIncludeOperation_withSingleValue() {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue("activity", "include", new String[]{"LOGIN"});

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).contains("LIKE '%login%'");
        assertThat(sql.toString()).doesNotContain(" OR ");
    }

    // -------- helper --------

    private void assertDateFilter(String operation, String expected) {
        StringBuilder sql = new StringBuilder("SELECT * FROM action_log");

        FilterValue filter = new FilterValue(
                "createdDateTime",
                operation,
                new String[]{"2024-01-01"}
        );

        queryBuilder.buildActionLogFiltration(List.of(filter), sql, TABLE);

        assertThat(sql.toString()).contains(expected);
    }
}
