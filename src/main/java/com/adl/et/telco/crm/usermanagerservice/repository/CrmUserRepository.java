package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.dto.authentication.ExternalUserResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.user.AllUserDetails;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Repository
public interface CrmUserRepository extends JpaRepository<Users, Long> {

    // Oracle: requires sequence UMS_UUID_SEQ to exist in the schema
    // (CREATE SEQUENCE ums_uuid_seq START WITH 1 INCREMENT BY 1 NOCACHE).
    @Query(value = "SELECT ums_uuid_seq.NEXTVAL FROM dual", nativeQuery = true)
    Long generateUuidShort();

    @Query(nativeQuery = true)
    ExternalUserResponse findByUserName(@Param("userName") String userName);

    Optional<Users> findByEmail(String email);

    @Query(value = "select * from users where user_name = ?1", nativeQuery = true)
    Optional<Users> getUserForAuthByUserName(String userName);

    @Modifying
    @Transactional
    @Query(nativeQuery = true)
    int updateCrmUserLastLogTimeAndTid(@Param("logTime") String logTime, @Param("tid") String tid, @Param("userName") String userName);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN EXISTS(SELECT u.id\n" +
            "              FROM   users u\n" +
            "                     INNER JOIN user_to_roles utr\n" +
            "                             ON u.id = utr.user_id\n" +
            "                                AND LOWER(u.email) = LOWER(:email)\n" +
            "                                AND u.status_id NOT IN ( :validStatusForCreateUser )\n" +
            "                     INNER JOIN roles r\n" +
            "                             ON utr.role_id = r.id\n" +
            "                                AND r.tenant_id = :tenantId) THEN 1 ELSE 0 END FROM dual  ")
    int isExistingUser(String email, List<Long> validStatusForCreateUser, long tenantId);

    @Query(nativeQuery = true)
    List<AllUserDetails> getAllUsers(int limit, int offset, String userName, Long roleId, Long statusId, long tenantId);

    @Query(nativeQuery = true, value = "SELECT \n" +
            "  u.* \n" +
            "FROM \n" +
            "  users u \n" +
            "  INNER JOIN user_to_roles utr ON u.id = utr.user_id \n" +
            "  INNER JOIN roles r ON utr.role_id = r.id \n" +
            "WHERE \n" +
            "  u.status_id <> 3 \n" +
            "  AND LOWER(u.email) = LOWER(:email) \n" +
            "  AND r.tenant_id = :tenantId")
    Optional<Users> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") long tenantId);

    boolean existsByUserName(String userName);

    @Query(nativeQuery = true, value = "select\n" +
            "\tu.user_name\n" +
            "from\n" +
            "\tusers u\n" +
            "inner join user_to_roles utr on\n" +
            "\tu.id = utr.user_id\n" +
            "\tand utr.role_id in (\n" +
            "\tselect\n" +
            "\t\tid\n" +
            "\tfrom\n" +
            "\t\troles r\n" +
            "\twhere\n" +
            "\t\tr.tenant_id = :tenantId)")
    List<String> findByTenantId(long tenantId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "update\n" +
            "\tusers\n" +
            "set\n" +
            "\tstatus_id = 2\n" +
            "where\n" +
            "\tusers.status_id = 1\n" +
            "\tand TRUNC(SYSDATE) - TO_DATE(REGEXP_SUBSTR(users.last_login_datetime, '^[^.]+'), 'YYYY-MM-DD HH24:MI:SS') > :days\n" +
            "\tand EXISTS (\n" +
            "\tselect\n" +
            "\t\t1\n" +
            "\tfrom\n" +
            "\t\tuser_to_roles utr\n" +
            "\twhere\n" +
            "\t\tutr.user_id = users.id\n" +
            "\t\tand utr.role_id in (\n" +
            "\t\tselect\n" +
            "\t\t\tid\n" +
            "\t\tfrom\n" +
            "\t\t\troles r\n" +
            "\t\twhere\n" +
            "\t\t\tr.tenant_id = :tenantId))")
    void expireUser(Integer days, long tenantId);

    @Query(nativeQuery = true)
    List<AllUserDetails> getAllFilteredUser(String userId, String userIdFilterType,
                                            String name, String nameFilterType,
                                            String email, String emailFilterType,
                                            List<String> statusList,
                                            String roleName, String roleNameFilterType,
                                            String userAccount, String userAccountFilterType,
                                            String mobileNumber, String mobileNumberFilterType,
                                            String lastLoginTime, String lastLoginTimeFilterType,
                                            String userType, String userTypeFilterType,
                                            String defaultGroup, String defaultGroupFilterType,
                                            String userGroup, String userGroupFilterType,
                                            String createdDate, String createdDateFilterType,
                                            int limit, int offset,
                                            long tenantId);

    // CrmUserRepository.java
    @Query(nativeQuery = true, value = "SELECT CASE WHEN EXISTS (" +
            "SELECT 1 FROM users u " +
            "INNER JOIN user_to_roles utr ON u.id = utr.user_id " +
            "INNER JOIN roles r ON utr.role_id = r.id AND r.tenant_id = :tenantId " +
            "WHERE LOWER(u.email) = LOWER(:email)) THEN 1 ELSE 0 END FROM dual")
    Long existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM users", nativeQuery = true)
    Long getNextUserId();
}