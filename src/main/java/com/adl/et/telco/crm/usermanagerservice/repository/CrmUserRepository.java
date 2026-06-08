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

    @Query(value = "SELECT UUID_SHORT()", nativeQuery = true)
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

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT u.id\n" +
            "              FROM   users u\n" +
            "                     INNER JOIN user_to_roles utr\n" +
            "                             ON u.id = utr.user_id\n" +
            "                                AND LOWER(u.email) = LOWER(:email)\n" +
            "                                AND u.status_id NOT IN ( :validStatusForCreateUser )\n" +
            "                     INNER JOIN roles r\n" +
            "                             ON utr.role_id = r.id\n" +
            "                                AND r.tenant_id = :tenantId)  ")
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
            "\tusers JOIN user_to_roles  ON users.id = user_to_roles.user_id \n" +
            "set\n" +
            "\tusers.status_id = 2\n" +
            "where\n" +
            "\tusers.status_id = 1\n" +
            "\tand TIMESTAMPDIFF(day,\n" +
            "\tSTR_TO_DATE(SUBSTRING_INDEX(users.last_login_datetime, '.', 1), '%Y-%m-%d %H:%i:%s'),\n" +
            "\tCURDATE()) > :days\n" +
            "\tand user_to_roles.role_id  in (\n" +
            "\tselect\n" +
            "\t\tid\n" +
            "\tfrom\n" +
            "\t\troles r\n" +
            "\twhere\n" +
            "\t\tr.tenant_id = :tenantId)")
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
    @Query(nativeQuery = true, value = "SELECT EXISTS (" +
            "SELECT 1 FROM users u " +
            "INNER JOIN user_to_roles utr ON u.id = utr.user_id " +
            "INNER JOIN roles r ON utr.role_id = r.id AND r.tenant_id = :tenantId " +
            "WHERE LOWER(u.email) = LOWER(:email))")
    Long existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM users", nativeQuery = true)
    Long getNextUserId();
}