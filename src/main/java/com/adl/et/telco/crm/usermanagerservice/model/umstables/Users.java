package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import com.adl.et.telco.crm.usermanagerservice.dto.authentication.ExternalUserResponse;
import com.adl.et.telco.crm.usermanagerservice.dto.user.AllUserDetails;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ums_users")
@SqlResultSetMapping(
        name = "crmUserDetailMapping",
        classes = {
                @ConstructorResult(
                        targetClass = ExternalUserResponse.class,
                        columns = {
                                @ColumnResult(name = "password"),
                                @ColumnResult(name = "mobile_number"),
                                @ColumnResult(name = "status_id", type = Double.class),
                                @ColumnResult(name = "last_login_datetime")
                        }
                )
        }
)
@SqlResultSetMapping(
        name = "getAllUsersMapping",
        classes = {
                @ConstructorResult(
                        targetClass = AllUserDetails.class,
                        columns = {
                                @ColumnResult(name = "userId"),
                                @ColumnResult(name = "name"),
                                @ColumnResult(name = "username"),
                                @ColumnResult(name = "email"),
                                @ColumnResult(name = "status"),
                                @ColumnResult(name = "roleName"),
                                @ColumnResult(name = "userAccount"),
                                @ColumnResult(name = "mobileNumber"),
                                @ColumnResult(name = "lastLoginTime"),
                                @ColumnResult(name = "userType"),
                                @ColumnResult(name = "defaultGroup"),
                                @ColumnResult(name = "userGroup"),
                                @ColumnResult(name = "createdBy"),
                                @ColumnResult(name = "createdDate"),
                                @ColumnResult(name = "totalCount", type = Integer.class)
                        }
                )
        }
)

@SqlResultSetMapping(
        name = "getAllFilteredUserMapping",
        classes = {
                @ConstructorResult(
                        targetClass = AllUserDetails.class,
                        columns = {
                                @ColumnResult(name = "userId"),
                                @ColumnResult(name = "name"),
                                @ColumnResult(name = "username"),
                                @ColumnResult(name = "email"),
                                @ColumnResult(name = "status"),
                                @ColumnResult(name = "roleName"),
                                @ColumnResult(name = "userAccount"),
                                @ColumnResult(name = "mobileNumber"),
                                @ColumnResult(name = "lastLoginTime"),
                                @ColumnResult(name = "userType"),
                                @ColumnResult(name = "defaultGroup"),
                                @ColumnResult(name = "userGroup"),
                                @ColumnResult(name = "createdBy"),
                                @ColumnResult(name = "createdDate"),
                                @ColumnResult(name = "totalCount", type = Integer.class)
                        }
                )
        }
)

@NamedNativeQuery(
        name = "Users.findByUserName",
        resultSetMapping = "crmUserDetailMapping",
        query = "select password,mobile_number,status_id,last_login_datetime from ums_users where user_name=:userName"
)
@NamedNativeQuery(
        name = "Users.updateCrmUserLastLogTimeAndTid",
        query = "update ums_users set last_login_datetime = :logTime,session_id=:tid where user_name = :userName"
)
@NamedNativeQuery(
        name = "Users.getAllUsers",
        resultSetMapping = "getAllUsersMapping",
        query = "SELECT\n" +
                "    t.userId,\n" +
                "    t.name,\n" +
                "    t.username,\n" +
                "    t.email,\n" +
                "    t.status,\n" +
                "    t.roleName,\n" +
                "    t.userAccount,\n" +
                "    t.mobileNumber,\n" +
                "    t.lastLoginTime,\n" +
                "    t.userType,\n" +
                "    t.defaultGroup,\n" +
                "    t.userGroup,\n" +
                "    t.createdBy,\n" +
                "    t.createdDate,\n" +
                "    total_count.totalCount\n" +
                "FROM (\n" +
                "    SELECT\n" +
                "        TO_CHAR(u.id) AS userId,\n" +
                "        u.name,\n" +
                "        u.user_name AS username,\n" +
                "        u.email,\n" +
                "        s.name AS status,\n" +
                "        r.name AS roleName,\n" +
                "        u.user_account as userAccount,\n" +
                "        u.mobile_number as mobileNumber,\n" +
                "        u.last_login_datetime as lastLoginTime,\n" +
                "        u.user_type as userType,\n" +
                "        u.default_group as defaultGroup,\n" +
                "        u.associated_user_group as userGroup,\n" +
                "        COALESCE(creator.name, 'SailPoint') as createdBy,\n" +
                "        TO_CHAR(u.created_datetime, 'YYYY-MM-DD HH24:MI:SS') as createdDate\n" +
                "    FROM ums_users u\n" +
                "    LEFT JOIN ums_users creator ON u.created_by = creator.id\n" +
                "    INNER JOIN ums_status s\n" +
                "        ON u.status_id = s.id\n" +
                "        AND (:statusId IS NULL OR s.id = :statusId)\n" +
                "        AND u.status_id <> 3\n" +
                "        AND (:userName IS NULL OR u.name LIKE '%' || :userName || '%')\n" +
                "    INNER JOIN ums_user_to_roles utr\n" +
                "        ON u.id = utr.user_id\n" +
                "        AND (:roleId IS NULL OR utr.role_id = :roleId)\n" +
                "    INNER JOIN ums_roles r\n" +
                "        ON utr.role_id = r.id\n" +
                "        AND r.tenant_id = :tenantId\n" +
                ") t\n" +
                "CROSS JOIN (\n" +
                "    SELECT\n" +
                "        COUNT(*) AS totalCount\n" +
                "    FROM ums_users u\n" +
                "    INNER JOIN ums_status s\n" +
                "        ON u.status_id = s.id\n" +
                "        AND (:statusId IS NULL OR s.id = :statusId)\n" +
                "        AND u.status_id <> 3\n" +
                "        AND (:userName IS NULL OR u.name LIKE '%' || :userName || '%')\n" +
                "    INNER JOIN ums_user_to_roles utr\n" +
                "        ON u.id = utr.user_id\n" +
                "        AND (:roleId IS NULL OR utr.role_id = :roleId)\n" +
                "    INNER JOIN ums_roles r\n" +
                "        ON utr.role_id = r.id\n" +
                "        AND r.tenant_id = :tenantId\n" +
                ") total_count\n" +
                "ORDER BY TO_NUMBER(t.userId) DESC\n" +
                "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY"
)

@NamedNativeQuery(
        name = "Users.getAllFilteredUser",
        resultSetMapping = "getAllFilteredUserMapping",
        query = "SELECT\n" +
                "    t.userId,\n" +
                "    t.name,\n" +
                "    t.username,\n" +
                "    t.email,\n" +
                "    t.status,\n" +
                "    t.roleName,\n" +
                "    t.userAccount,\n" +
                "    t.mobileNumber,\n" +
                "    t.lastLoginTime,\n" +
                "    t.userType,\n" +
                "    t.defaultGroup,\n" +
                "    t.userGroup,\n" +
                "    t.createdBy,\n" +
                "    t.createdDate,\n" +
                "    total_count.totalCount\n" +
                "FROM (\n" +
                "    SELECT\n" +
                "        TO_CHAR(u.id) AS userId,\n" +
                "        u.name,\n" +
                "        u.user_name AS username,\n" +
                "        u.email,\n" +
                "        s.name AS status,\n" +
                "        r.name AS roleName,\n" +
                "        u.user_account AS userAccount,\n" +
                "        u.mobile_number AS mobileNumber,\n" +
                "        u.last_login_datetime AS lastLoginTime,\n" +
                "        u.user_type AS userType,\n" +
                "        u.default_group AS defaultGroup,\n" +
                "        u.associated_user_group AS userGroup,\n" +
                "        COALESCE(creator.name, 'System') AS createdBy,\n" +
                "        TO_CHAR(u.created_datetime, 'YYYY-MM-DD HH24:MI:SS') AS createdDate\n" +
                "    FROM ums_users u\n" +
                "    LEFT JOIN ums_users creator ON u.created_by = creator.id\n" +
                "    INNER JOIN ums_status s ON u.status_id = s.id\n" +
                "    INNER JOIN ums_user_to_roles utr ON u.id = utr.user_id\n" +
                "    INNER JOIN ums_roles r ON utr.role_id = r.id AND r.tenant_id = :tenantId\n" +
                "    WHERE u.status_id <> 3\n" +
                "      AND (\n" +
                "          (:userId IS NULL AND :userIdFilterType IS NULL) OR\n" +
                "          (:userIdFilterType = 'Equal' AND TO_CHAR(u.id) = :userId) OR\n" +
                "          (:userIdFilterType = 'Include' AND TO_CHAR(u.id) LIKE '%' || :userId || '%') OR\n" +
                "          (:userIdFilterType = 'Is empty' AND u.id IS NULL)\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:name IS NULL AND :nameFilterType IS NULL) OR\n" +
                "          (:nameFilterType = 'Equal' AND LOWER(u.name) = LOWER(:name)) OR\n" +
                "          (:nameFilterType = 'Include' AND LOWER(u.name) LIKE '%' || LOWER(:name) || '%') OR\n" +
                "          (:nameFilterType = 'Is empty' AND (u.name IS NULL OR u.name = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:email IS NULL AND :emailFilterType IS NULL) OR\n" +
                "          (:emailFilterType = 'Equal' AND LOWER(u.email) = LOWER(:email)) OR\n" +
                "          (:emailFilterType = 'Include' AND LOWER(u.email) LIKE '%' || LOWER(:email) || '%') OR\n" +
                "          (:emailFilterType = 'Is empty' AND (u.email IS NULL OR u.email = ''))\n" +
                "      )\n" +
                "      AND (s.name IN (:statusList))\n" +
                "      AND (\n" +
                "          (:roleName IS NULL AND :roleNameFilterType IS NULL) OR\n" +
                "          (:roleNameFilterType = 'Equal' AND LOWER(r.name) = LOWER(:roleName)) OR\n" +
                "          (:roleNameFilterType = 'Include' AND LOWER(r.name) LIKE '%' || LOWER(:roleName) || '%') OR\n" +
                "          (:roleNameFilterType = 'Is empty' AND (r.name IS NULL OR r.name = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:userAccount IS NULL AND :userAccountFilterType IS NULL) OR\n" +
                "          (:userAccountFilterType = 'Equal' AND LOWER(u.user_account) = LOWER(:userAccount)) OR\n" +
                "          (:userAccountFilterType = 'Include' AND LOWER(u.user_account) LIKE '%' || LOWER(:userAccount) || '%') OR\n" +
                "          (:userAccountFilterType = 'Is empty' AND (u.user_account IS NULL OR u.user_account = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:mobileNumber IS NULL AND :mobileNumberFilterType IS NULL) OR\n" +
                "          (:mobileNumberFilterType = 'Equal' AND LOWER(u.mobile_number) = LOWER(:mobileNumber)) OR\n" +
                "          (:mobileNumberFilterType = 'Include' AND LOWER(u.mobile_number) LIKE '%' || LOWER(:mobileNumber) || '%') OR\n" +
                "          (:mobileNumberFilterType = 'Is empty' AND (u.mobile_number IS NULL OR u.mobile_number = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:lastLoginTime IS NULL AND :lastLoginTimeFilterType IS NULL) OR\n" +
                "          (:lastLoginTimeFilterType = 'Equal' AND u.last_login_datetime = :lastLoginTime) OR\n" +
                "          (:lastLoginTimeFilterType = 'Is empty' AND u.last_login_datetime IS NULL)\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:userType IS NULL AND :userTypeFilterType IS NULL) OR\n" +
                "          (:userTypeFilterType = 'Equal' AND LOWER(u.user_type) = LOWER(:userType)) OR\n" +
                "          (:userTypeFilterType = 'Include' AND LOWER(u.user_type) LIKE '%' || LOWER(:userType) || '%') OR\n" +
                "          (:userTypeFilterType = 'Is empty' AND (u.user_type IS NULL OR u.user_type = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:defaultGroup IS NULL AND :defaultGroupFilterType IS NULL) OR\n" +
                "          (:defaultGroupFilterType = 'Equal' AND LOWER(u.default_group) = LOWER(:defaultGroup)) OR\n" +
                "          (:defaultGroupFilterType = 'Include' AND LOWER(u.default_group) LIKE '%' || LOWER(:defaultGroup) || '%') OR\n" +
                "          (:defaultGroupFilterType = 'Is empty' AND (u.default_group IS NULL OR u.default_group = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:userGroup IS NULL AND :userGroupFilterType IS NULL) OR\n" +
                "          (:userGroupFilterType = 'Equal' AND LOWER(u.associated_user_group) = LOWER(:userGroup)) OR\n" +
                "          (:userGroupFilterType = 'Include' AND LOWER(u.associated_user_group) LIKE '%' || LOWER(:userGroup) || '%') OR\n" +
                "          (:userGroupFilterType = 'Is empty' AND (u.associated_user_group IS NULL OR u.associated_user_group = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:createdDate IS NULL AND :createdDateFilterType IS NULL) OR\n" +
                "          (:createdDateFilterType = 'Equal' AND TO_CHAR(u.created_datetime, 'YYYY-MM-DD HH24:MI:SS') = :createdDate) OR\n" +
                "          (:createdDateFilterType = 'Include' AND TO_CHAR(u.created_datetime, 'YYYY-MM-DD HH24:MI:SS') LIKE '%' || :createdDate || '%') OR\n" +
                "          (:createdDateFilterType = 'Is empty' AND u.created_datetime IS NULL)\n" +
                "      )\n" +
                ") t\n" +
                "CROSS JOIN (\n" +
                "    SELECT COUNT(*) AS totalCount\n" +
                "    FROM ums_users u\n" +
                "    INNER JOIN ums_status s ON u.status_id = s.id\n" +
                "    INNER JOIN ums_user_to_roles utr ON u.id = utr.user_id\n" +
                "    INNER JOIN ums_roles r ON utr.role_id = r.id AND r.tenant_id = :tenantId\n" +
                "    WHERE u.status_id <> 3\n" +
                "      AND (\n" +
                "          (:userId IS NULL AND :userIdFilterType IS NULL) OR\n" +
                "          (:userIdFilterType = 'Equal' AND TO_CHAR(u.id) = :userId) OR\n" +
                "          (:userIdFilterType = 'Include' AND TO_CHAR(u.id) LIKE '%' || :userId || '%') OR\n" +
                "          (:userIdFilterType = 'Is empty' AND u.id IS NULL)\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:name IS NULL AND :nameFilterType IS NULL) OR\n" +
                "          (:nameFilterType = 'Equal' AND LOWER(u.name) = LOWER(:name)) OR\n" +
                "          (:nameFilterType = 'Include' AND LOWER(u.name) LIKE '%' || LOWER(:name) || '%') OR\n" +
                "          (:nameFilterType = 'Is empty' AND (u.name IS NULL OR u.name = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:email IS NULL AND :emailFilterType IS NULL) OR\n" +
                "          (:emailFilterType = 'Equal' AND LOWER(u.email) = LOWER(:email)) OR\n" +
                "          (:emailFilterType = 'Include' AND LOWER(u.email) LIKE '%' || LOWER(:email) || '%') OR\n" +
                "          (:emailFilterType = 'Is empty' AND (u.email IS NULL OR u.email = ''))\n" +
                "      )\n" +
                "      AND (s.name IN (:statusList))\n" +
                "      AND (\n" +
                "          (:roleName IS NULL AND :roleNameFilterType IS NULL) OR\n" +
                "          (:roleNameFilterType = 'Equal' AND LOWER(r.name) = LOWER(:roleName)) OR\n" +
                "          (:roleNameFilterType = 'Include' AND LOWER(r.name) LIKE '%' || LOWER(:roleName) || '%') OR\n" +
                "          (:roleNameFilterType = 'Is empty' AND (r.name IS NULL OR r.name = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:userAccount IS NULL AND :userAccountFilterType IS NULL) OR\n" +
                "          (:userAccountFilterType = 'Equal' AND LOWER(u.user_account) = LOWER(:userAccount)) OR\n" +
                "          (:userAccountFilterType = 'Include' AND LOWER(u.user_account) LIKE '%' || LOWER(:userAccount) || '%') OR\n" +
                "          (:userAccountFilterType = 'Is empty' AND (u.user_account IS NULL OR u.user_account = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:mobileNumber IS NULL AND :mobileNumberFilterType IS NULL) OR\n" +
                "          (:mobileNumberFilterType = 'Equal' AND LOWER(u.mobile_number) = LOWER(:mobileNumber)) OR\n" +
                "          (:mobileNumberFilterType = 'Include' AND LOWER(u.mobile_number) LIKE '%' || LOWER(:mobileNumber) || '%') OR\n" +
                "          (:mobileNumberFilterType = 'Is empty' AND (u.mobile_number IS NULL OR u.mobile_number = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:lastLoginTime IS NULL AND :lastLoginTimeFilterType IS NULL) OR\n" +
                "          (:lastLoginTimeFilterType = 'Equal' AND u.last_login_datetime = :lastLoginTime) OR\n" +
                "          (:lastLoginTimeFilterType = 'Is empty' AND u.last_login_datetime IS NULL)\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:userType IS NULL AND :userTypeFilterType IS NULL) OR\n" +
                "          (:userTypeFilterType = 'Equal' AND LOWER(u.user_type) = LOWER(:userType)) OR\n" +
                "          (:userTypeFilterType = 'Include' AND LOWER(u.user_type) LIKE '%' || LOWER(:userType) || '%') OR\n" +
                "          (:userTypeFilterType = 'Is empty' AND (u.user_type IS NULL OR u.user_type = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:defaultGroup IS NULL AND :defaultGroupFilterType IS NULL) OR\n" +
                "          (:defaultGroupFilterType = 'Equal' AND LOWER(u.default_group) = LOWER(:defaultGroup)) OR\n" +
                "          (:defaultGroupFilterType = 'Include' AND LOWER(u.default_group) LIKE '%' || LOWER(:defaultGroup) || '%') OR\n" +
                "          (:defaultGroupFilterType = 'Is empty' AND (u.default_group IS NULL OR u.default_group = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:userGroup IS NULL AND :userGroupFilterType IS NULL) OR\n" +
                "          (:userGroupFilterType = 'Equal' AND LOWER(u.associated_user_group) = LOWER(:userGroup)) OR\n" +
                "          (:userGroupFilterType = 'Include' AND LOWER(u.associated_user_group) LIKE '%' || LOWER(:userGroup) || '%') OR\n" +
                "          (:userGroupFilterType = 'Is empty' AND (u.associated_user_group IS NULL OR u.associated_user_group = ''))\n" +
                "      )\n" +
                "      AND (\n" +
                "          (:createdDate IS NULL AND :createdDateFilterType IS NULL) OR\n" +
                "          (:createdDateFilterType = 'Equal' AND TO_CHAR(u.created_datetime, 'YYYY-MM-DD HH24:MI:SS') = :createdDate) OR\n" +
                "          (:createdDateFilterType = 'Include' AND TO_CHAR(u.created_datetime, 'YYYY-MM-DD HH24:MI:SS') LIKE '%' || :createdDate || '%') OR\n" +
                "          (:createdDateFilterType = 'Is empty' AND u.created_datetime IS NULL)\n" +
                "      )\n" +
                ") total_count\n" +
                "ORDER BY TO_NUMBER(t.userId) DESC\n" +
                "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY"
)

public class Users implements Comparable<Users> {
    @Id
    private Long id;
    private String name;
    private String email;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Users createdBy;
    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdDatetime;


    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Users updatedBy;
    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp updatedDatetime;
    private String userName;
    private String password;
    private String mobileNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STATUS_ID")
    private Status status;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "ums_USER_TO_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private List<Roles> roles;
    private String lastLoginDatetime;
    private String sessionId;
    private String userAccount;
    private String userType;
    private String defaultGroup;
    private String associatedUserGroup;


    @Override
    public String toString() {
        return "User{" +
                ", userName='" + userName + '\'' +
                ", fullName='" + name + '\'' +
                ", lastLogin='" + lastLoginDatetime + '\'' +
                '}';
    }

    @Override
    public int compareTo(Users users) {
        return this.userName.compareTo(users.getUserName());
    }

}

