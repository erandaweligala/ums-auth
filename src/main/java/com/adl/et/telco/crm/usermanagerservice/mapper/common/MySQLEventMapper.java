package com.adl.et.telco.crm.usermanagerservice.mapper.common;

import com.adl.et.telco.crm.usermanagerservice.client.MySQLKafkaEventPublisher;
import com.adl.et.telco.crm.usermanagerservice.dto.common.DBWriteRequestMySQL;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.CreatePermission;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.EditPermission;
import com.adl.et.telco.crm.usermanagerservice.dto.role.CreateNewRole;
import com.adl.et.telco.crm.usermanagerservice.dto.role.UpdateRole;
import com.adl.et.telco.crm.usermanagerservice.dto.user.CreateUserRequest;
import com.adl.et.telco.crm.usermanagerservice.dto.user.EditUserRequest;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.ActionLog;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Users;
import com.adl.et.telco.crm.usermanagerservice.repository.CrmUserRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.PermissionsRepository;
import com.adl.et.telco.crm.usermanagerservice.repository.RoleRepository;
import com.adl.et.telco.crm.usermanagerservice.util.exception.BaseException;
import com.adl.et.telco.crm.usermanagerservice.util.resultenum.ResponseCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Builds and publishes {@link DBWriteRequestMySQL} Kafka events for every
 * UMS write operation.
 *
 * <h3>Key design notes</h3>
 * <ul>
 *   <li>No direct DB writes from UMS — everything goes via Kafka to db-write-service.</li>
 *
 *   <li><b>All CREATE flows</b> now use a single-publish pattern:
 *       The parent row id is generated upfront via {@code getNextXxxId()} (a direct DB call,
 *       not via Kafka), so the id is known before publishing. The parent INSERT and all
 *       join-table INSERTs are bundled as {@code relatedWrites} on one Kafka message.
 *       This eliminates the old two-step INSERT → SELECT-back pattern which was
 *       broken under Kafka lag.</li>
 *
 *   <li><b>EDIT flows</b> continue to bundle all DML (UPDATE parent + DELETE old +
 *       INSERT new) as {@code relatedWrites} on the parent UPDATE — unchanged.</li>
 *
 *   <li>Id generation uses {@code SELECT COALESCE(MAX(id), 0) + 1} via a native JPA
 *       query ({@code getNextUserId()}, {@code getNextRoleId()},
 *       {@code getNextPermissionId()}) on the respective repository. This is portable
 *       SQL with no database-specific functions, so it runs unchanged on Oracle.</li>
 *
 *   <li>{@code DBWriteRequestMySQL} uses plain {@code @Builder} —
 *       {@code toBuilder()} is not available.</li>
 *
 *   <li>{@link BaseException} requires all 5 constructor arguments.</li>
 * </ul>
 *
 * <h3>Id columns (users, roles, permission)</h3>
 * <pre>
 * The {@code id} of users / roles / permission is assigned by the application
 * (see getNextXxxId() above) and persisted with @Id only — no @GeneratedValue.
 * On Oracle the column is a plain NUMBER; no identity/sequence is required for
 * these three tables.
 * </pre>
 *
 * <h3>Table / column reference (lowercase snake_case)</h3>
 * <pre>
 * users                    id, name, email, mobile_number, user_name,
 *                          status_id, user_account, user_type, default_group,
 *                          associated_user_group, created_by, updated_by
 * user_to_roles            user_id, role_id
 * roles                    id, name, description, tenant_id, status_id,
 *                          created_by, updated_by
 * role_to_permissions      role_id, permission_id
 * permission               id, name, description, menu_id, component_id,
 *                          tenant_id, status_id
 * permission_to_actions    permission_id, action_id
 * permission_to_attributes permission_id, attribute_id
 * action_log               id, activity_id, activity, status,
 *                          status_description, user, detailed_request,
 *                          created_at, updated_at, url, action_id, activity_type
 * </pre>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MySQLEventMapper {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final String INSERT       = "INSERT";
    private static final String UPDATE       = "UPDATE";
    private static final String DELETE       = "DELETE";
    private static final String NATIVE_QUERY = "NATIVE_QUERY";

    private static final String T_USERS               = "ums_users";
    private static final String T_USER_TO_ROLES       = "ums_user_to_roles";
    private static final String T_ROLES               = "ums_roles";
    private static final String T_ROLE_TO_PERMISSIONS = "ums_role_to_permissions";
    private static final String T_PERMISSION          = "ums_permission";
    private static final String T_PERM_TO_ACTIONS     = "ums_permission_to_actions";
    private static final String T_PERM_TO_ATTRIBUTES  = "ums_permission_to_attributes";
    private static final String T_ACTION_LOG          = "ums_action_log";

    private final MySQLKafkaEventPublisher publisher;
    private final CrmUserRepository        crmUserRepository;
    private final RoleRepository           roleRepository;
    private final PermissionsRepository    permissionsRepository;

    // =========================================================================
    // Users
    // =========================================================================

    /**
     * Single-publish CREATE flow for users + user_to_roles.
     *
     * <p>The user id is generated upfront via {@code getNextUserId()} so both the
     * parent INSERT and the join-table INSERT can be bundled as {@code relatedWrites}
     * in one Kafka message. No SELECT-back after publish is needed.
     *
     * <p>db-write-service will execute both INSERTs in one transaction:
     * if the parent INSERT is a duplicate (0 rows), the join-table INSERT is
     * automatically skipped.
     */
    public void publishCreateUser(CreateUserRequest request,
                                  String actor,
                                  String normalizedEmail) throws BaseException {

        long userId = crmUserRepository.getNextUserId();

        Map<String, Object> userCols = new LinkedHashMap<>();
        userCols.put("id",                    userId);
        userCols.put("name",                  request.getName());
        userCols.put("email",                 normalizedEmail);
        userCols.put("mobile_number",         request.getMobileNumber());
        userCols.put("user_account",          request.getUserAccount());
        userCols.put("user_type",             request.getUserType());
        userCols.put("default_group",         request.getDefaultGroup());
        userCols.put("associated_user_group", request.getAssociatedUserGroup());
        userCols.put("status_id",             Long.parseLong(request.getStatus()));
        userCols.put("created_by",            resolveUserId(request.getCreatedBy()));
        userCols.put("user_name",             extractUserName(normalizedEmail));

        DBWriteRequestMySQL insertUserToRoles = DBWriteRequestMySQL.builder()
                .eventType(INSERT)
                .timestamp(now())
                .userName(actor)
                .tableName(T_USER_TO_ROLES)
                .columnValues(Map.of(
                        "user_id", userId,
                        "role_id", Long.parseLong(request.getRoleId())
                ))
                .build();

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_USERS)
                        .columnValues(userCols)
                        .relatedWrites(List.of(insertUserToRoles))
                        .build(),
                "createUser"
        );
    }

    /**
     * Edit flow for users — all DML in one atomic publish via relatedWrites:
     * UPDATE users → DELETE old user_to_roles → INSERT new user_to_roles.
     *
     * <p>Safe to use relatedWrites here: the parent UPDATE targets a specific
     * user id and will always return rowCount=1 on a valid request.
     */
    public void publishEditUser(EditUserRequest request, String actor) throws BaseException {

        long userId = Long.parseLong(request.getUserId());

        Map<String, Object> userCols = new LinkedHashMap<>();
        userCols.put("name",                  request.getName());
        userCols.put("mobile_number",         request.getMobileNumber());
        userCols.put("user_type",             request.getUserType());
        userCols.put("default_group",         request.getDefaultGroup());
        userCols.put("associated_user_group", request.getAssociatedUserGroup());
        userCols.put("status_id",             Long.parseLong(request.getStatus()));
        userCols.put("updated_by",            resolveUserId(request.getUpdatedBy()));

        DBWriteRequestMySQL deleteOldRoles = DBWriteRequestMySQL.builder()
                .eventType(DELETE)
                .timestamp(now())
                .userName(actor)
                .tableName(T_USER_TO_ROLES)
                .whereConditions(Map.of("user_id", userId))
                .build();

        DBWriteRequestMySQL insertNewRole = DBWriteRequestMySQL.builder()
                .eventType(INSERT)
                .timestamp(now())
                .userName(actor)
                .tableName(T_USER_TO_ROLES)
                .columnValues(Map.of(
                        "user_id", userId,
                        "role_id", Long.parseLong(request.getRoleId())
                ))
                .build();

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(UPDATE)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_USERS)
                        .columnValues(userCols)
                        .whereConditions(Map.of("id", userId))
                        .relatedWrites(List.of(deleteOldRoles, insertNewRole))
                        .build(),
                "editUser"
        );
    }

    /**
     * Expire users — NATIVE_QUERY because the WHERE clause requires a JOIN
     * and MySQL date functions that can't be expressed with structured maps.
     *
     * <p>Parameter positions (match ? placeholders in order):
     * <ol>
     *   <li>new status_id  (2 = expired)</li>
     *   <li>current status_id to match  (1 = active)</li>
     *   <li>days threshold</li>
     *   <li>tenantId</li>
     * </ol>
     */
    public void publishExpireUsers(Integer days, Long tenantId, String actor)
            throws BaseException {

        String sql =
                "UPDATE ums_users " +
                        "SET status_id = ? " +
                        "WHERE users.status_id = ? " +
                        "AND TRUNC(SYSDATE) - TO_DATE(" +
                        "    REGEXP_SUBSTR(users.last_login_datetime, '^[^.]+'), " +
                        "    'YYYY-MM-DD HH24:MI:SS') > ? " +
                        "AND EXISTS (SELECT 1 FROM ums_user_to_roles utr " +
                        "    WHERE utr.user_id = users.id " +
                        "    AND utr.role_id IN (SELECT id FROM ums_roles WHERE tenant_id = ?))";

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(NATIVE_QUERY)
                        .timestamp(now())
                        .userName(actor)
                        .nativeQuery(sql)
                        .queryParams(List.of(2, 1, days, tenantId))
                        .build(),
                "expireUsers"
        );
    }

    // =========================================================================
    // Roles
    // =========================================================================

    /**
     * Single-publish CREATE flow for roles + role_to_permissions.
     *
     * <p>The role id is generated upfront via {@code getNextRoleId()} so the parent
     * INSERT and all join-table INSERTs can be bundled as {@code relatedWrites}
     * in one Kafka message. No SELECT-back after publish is needed.
     *
     * <p>db-write-service executes all writes in one transaction. A duplicate
     * on any join-table row is silently skipped (rowCount=0); other failures
     * roll back the entire transaction.
     */
    public void publishCreateRole(CreateNewRole request,
                                  List<Long> permissionIds,
                                  Long tenantId,
                                  String actor) throws BaseException {

        long roleId = roleRepository.getNextRoleId();

        Map<String, Object> roleCols = new LinkedHashMap<>();
        roleCols.put("id",          roleId);
        roleCols.put("name",        request.getRoleName());
        roleCols.put("description", request.getDescription());
        roleCols.put("tenant_id",   tenantId);
        roleCols.put("status_id",   1L); // ACTIVE
        roleCols.put("created_by",  resolveUserId(request.getCreatedBy()));

        List<DBWriteRequestMySQL> relatedWrites = new ArrayList<>();

        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                relatedWrites.add(DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_ROLE_TO_PERMISSIONS)
                        .columnValues(Map.of(
                                "role_id",       roleId,
                                "permission_id", permId
                        ))
                        .build());
            }
        }

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_ROLES)
                        .columnValues(roleCols)
                        .relatedWrites(relatedWrites.isEmpty() ? null : relatedWrites)
                        .build(),
                "createRole"
        );
    }

    /**
     * Edit flow for roles — atomic via relatedWrites:
     * UPDATE roles → DELETE old role_to_permissions → INSERT new role_to_permissions.
     *
     * <p>Safe to use relatedWrites here: the parent UPDATE targets a specific
     * role id and will always return rowCount=1 on a valid request.
     */
    public void publishEditRole(UpdateRole request,
                                List<Long> permissionIds,
                                String actor) throws BaseException {

        long roleId = Long.parseLong(request.getRoleId());

        Map<String, Object> roleCols = new LinkedHashMap<>();
        roleCols.put("name",        request.getRoleName());
        roleCols.put("description", request.getDescription());
        roleCols.put("updated_by",  resolveUserId(request.getCreatedBy()));

        List<DBWriteRequestMySQL> related = new ArrayList<>();

        related.add(DBWriteRequestMySQL.builder()
                .eventType(DELETE)
                .timestamp(now())
                .userName(actor)
                .tableName(T_ROLE_TO_PERMISSIONS)
                .whereConditions(Map.of("role_id", roleId))
                .build());

        if (permissionIds != null) {
            for (Long permId : permissionIds) {
                related.add(DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_ROLE_TO_PERMISSIONS)
                        .columnValues(Map.of(
                                "role_id",       roleId,
                                "permission_id", permId
                        ))
                        .build());
            }
        }

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(UPDATE)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_ROLES)
                        .columnValues(roleCols)
                        .whereConditions(Map.of("id", roleId))
                        .relatedWrites(related)
                        .build(),
                "editRole"
        );
    }

    // =========================================================================
    // Permissions
    // =========================================================================

    /**
     * Single-publish CREATE flow for permission + join tables.
     *
     * <p>The permission id is generated upfront via {@code getNextPermissionId()} so the
     * parent INSERT and all join-table INSERTs can be bundled as {@code relatedWrites}
     * in one Kafka message. No SELECT-back after publish is needed.
     *
     * <p>db-write-service executes all writes in one transaction. A duplicate
     * on any join-table row is silently skipped; other failures roll back everything.
     */
    public void publishCreatePermission(CreatePermission request,
                                        Long tenantId,
                                        String actor) throws BaseException {

        long permissionId = permissionsRepository.getNextPermissionId();
        long menuId       = Long.parseLong(request.getMenuId());
        long componentId  = Long.parseLong(request.getComponentId());

        Map<String, Object> permCols = new LinkedHashMap<>();
        permCols.put("id",           permissionId);
        permCols.put("name",         request.getName());
        permCols.put("description",  request.getDescription());
        permCols.put("menu_id",      menuId);
        permCols.put("component_id", componentId);
        permCols.put("tenant_id",    tenantId);
        permCols.put("status_id",    1L); // ACTIVE

        List<DBWriteRequestMySQL> relatedWrites = new ArrayList<>();

        if (request.getActions() != null && !request.getActions().isEmpty()) {
            for (Long actionId : request.getActions()) {
                relatedWrites.add(DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_PERM_TO_ACTIONS)
                        .columnValues(Map.of(
                                "permission_id", permissionId,
                                "action_id",     actionId
                        ))
                        .build());
            }
        }

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (Long attributeId : request.getAttributes()) {
                relatedWrites.add(DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_PERM_TO_ATTRIBUTES)
                        .columnValues(Map.of(
                                "permission_id", permissionId,
                                "attribute_id",  attributeId
                        ))
                        .build());
            }
        }

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_PERMISSION)
                        .columnValues(permCols)
                        .relatedWrites(relatedWrites.isEmpty() ? null : relatedWrites)
                        .build(),
                "createPermission"
        );
    }

    /**
     * Edit flow for permissions — atomic via relatedWrites:
     * UPDATE permission → DELETE old actions → DELETE old attributes →
     * INSERT new actions → INSERT new attributes.
     *
     * <p>Safe to use relatedWrites here: the parent UPDATE targets a specific
     * permission id and will always return rowCount=1 on a valid request.
     */
    public void publishEditPermission(EditPermission request,
                                      Long tenantId,
                                      String actor) throws BaseException {

        long permissionId = Long.parseLong(request.getPermissionId());

        Map<String, Object> permCols = new LinkedHashMap<>();
        permCols.put("name",         request.getName());
        permCols.put("description",  request.getDescription());
        permCols.put("menu_id",      Long.parseLong(request.getMenuId()));
        permCols.put("component_id", Long.parseLong(request.getComponentId()));
        permCols.put("tenant_id",    tenantId);
        permCols.put("status_id",    1L); // ACTIVE

        List<DBWriteRequestMySQL> related = new ArrayList<>();

        related.add(DBWriteRequestMySQL.builder()
                .eventType(DELETE)
                .timestamp(now())
                .userName(actor)
                .tableName(T_PERM_TO_ACTIONS)
                .whereConditions(Map.of("permission_id", permissionId))
                .build());

        related.add(DBWriteRequestMySQL.builder()
                .eventType(DELETE)
                .timestamp(now())
                .userName(actor)
                .tableName(T_PERM_TO_ATTRIBUTES)
                .whereConditions(Map.of("permission_id", permissionId))
                .build());

        if (request.getActions() != null) {
            for (Long actionId : request.getActions()) {
                related.add(DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_PERM_TO_ACTIONS)
                        .columnValues(Map.of(
                                "permission_id", permissionId,
                                "action_id",     actionId
                        ))
                        .build());
            }
        }

        if (request.getAttributes() != null) {
            for (Long attributeId : request.getAttributes()) {
                related.add(DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_PERM_TO_ATTRIBUTES)
                        .columnValues(Map.of(
                                "permission_id", permissionId,
                                "attribute_id",  attributeId
                        ))
                        .build());
            }
        }

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(UPDATE)
                        .timestamp(now())
                        .userName(actor)
                        .tableName(T_PERMISSION)
                        .columnValues(permCols)
                        .whereConditions(Map.of("id", permissionId))
                        .relatedWrites(related)
                        .build(),
                "editPermission"
        );
    }

    // =========================================================================
    // Action Log
    // =========================================================================

    /**
     * Publishes an INSERT for a new action_log row.
     * action_log.id remains AUTO_INCREMENT — it is never referenced by a
     * join table so no upfront id generation is needed here.
     */
    public void publishCreateActionLog(ActionLog actionLog) throws BaseException {

        Map<String, Object> cols = new LinkedHashMap<>();
        cols.put("activity_id",        actionLog.getActivityId());
        cols.put("activity",           actionLog.getActivity());
        cols.put("status",             actionLog.getStatus());
        cols.put("status_description", actionLog.getStatusDescription());
        cols.put("user",               actionLog.getUser());
        cols.put("detailed_request",   actionLog.getDetailedRequest());
        cols.put("created_at",         formatDateTime(actionLog.getCreatedAt()));
        cols.put("updated_at",         formatDateTime(actionLog.getUpdatedAt()));
        cols.put("url",                actionLog.getUrl());
        cols.put("action_id",          actionLog.getActionId());
        cols.put("activity_type",      actionLog.getActivityType());

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(INSERT)
                        .timestamp(now())
                        .userName(actionLog.getUser() != null ? actionLog.getUser() : "system")
                        .tableName(T_ACTION_LOG)
                        .columnValues(cols)
                        .build(),
                "createActionLog"
        );
    }

    /**
     * Publishes an UPDATE for an existing action_log row.
     * Only {@code status}, {@code status_description}, and {@code updated_at}
     * are written — mirrors the repository's {@code updateStatus} query exactly.
     *
     * @param activityId        matched in the WHERE clause
     * @param status            new status value
     * @param statusDescription new status description
     * @param actor             username for Kafka message key (use "system" if unknown)
     */
    public void publishUpdateActionLog(String activityId,
                                       String status,
                                       String statusDescription,
                                       String actor) throws BaseException {

        Map<String, Object> cols = new LinkedHashMap<>();
        cols.put("status",             status);
        cols.put("status_description", statusDescription);
        cols.put("updated_at",         now());

        publish(
                DBWriteRequestMySQL.builder()
                        .eventType(UPDATE)
                        .timestamp(now())
                        .userName(actor != null ? actor : "system")
                        .tableName(T_ACTION_LOG)
                        .columnValues(cols)
                        .whereConditions(Map.of("activity_id", activityId))
                        .build(),
                "updateActionLog"
        );
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(FORMATTER) : null;
    }

    /**
     * Resolves a username string to its Long id by querying DC MySQL directly.
     * Returns null if the username is null/blank or not found — FK becomes NULL.
     *
     * <p>This is a direct DB read (not via Kafka) and is safe to call before
     * publishing — it does not depend on any previously published Kafka message
     * having been consumed.
     */
    private Long resolveUserId(String userName) {
        if (userName == null || userName.isBlank()) return null;
        return crmUserRepository.getUserForAuthByUserName(userName)
                .map(Users::getId)
                .orElse(null);
    }

    /** Extracts the local-part (before @) of an email address. */
    private String extractUserName(String email) {
        return email.split("@")[0];
    }

    /**
     * Publishes a fire-and-forget Kafka message and throws {@link BaseException}
     * if the underlying publish call raises an exception.
     */
    private void publish(DBWriteRequestMySQL request, String operation) throws BaseException {
        try {
            publisher.publish(request);
            log.debug("[MySQLEventMapper] {} — published", operation);
        } catch (Exception e) {
            log.error("[MySQLEventMapper] {} — publish failed: {}", operation, e.getMessage());
            throw new BaseException(
                    "MySQL Kafka publish failed: " + operation,
                    ResponseCodeEnum.OPERATION_FAILED.description(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.OPERATION_FAILED.code(),
                    null
            );
        }
    }
}