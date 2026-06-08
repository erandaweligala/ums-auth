package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleDetailsRepoDTO;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleView;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(
        name = "findRoleListMetaDataMapping",
        classes = {
                @ConstructorResult(
                        targetClass = MetaData.class,
                        columns = {
                                @ColumnResult(name = "label"),
                                @ColumnResult(name = "value")
                        }
                )
        }
)
@SqlResultSetMapping(
        name = "findRoleListViewMapping",
        classes = {
                @ConstructorResult(
                        targetClass = RoleView.class,
                        columns = {
                                @ColumnResult(name = "roleId"),
                                @ColumnResult(name = "name"),
                                @ColumnResult(name = "description"),
                                @ColumnResult(name = "totalCount", type = Integer.class)
                        }
                )
        }
)
@SqlResultSetMapping(
        name = "getRoleDetailsMapping",
        classes = {
                @ConstructorResult(
                        targetClass = RoleDetailsRepoDTO.class,
                        columns = {
                                @ColumnResult(name = "permissionId"),
                                @ColumnResult(name = "componentId"),
                                @ColumnResult(name = "roleId"),
                                @ColumnResult(name = "roleName"),
                                @ColumnResult(name = "menuId"),
                                @ColumnResult(name = "description"),
                                @ColumnResult(name = "menuName"),
                                @ColumnResult(name = "componentName"),
                                @ColumnResult(name = "permissionDescription"),
                                @ColumnResult(name = "permissionName")
                        }
                )
        }
)


@NamedNativeQuery(
        name = "Roles.findRoleListMetaData",
        resultSetMapping = "findRoleListMetaDataMapping",
        query = "select CAST(r.id as CHAR) as value, r.name as label from roles r  where r.tenant_id = :tenantId"
)
@NamedNativeQuery(
        name = "Roles.findRoleListView",
        resultSetMapping = "findRoleListViewMapping",
        query = "SELECT\n" +
                "\tCAST(R.ID AS CHAR) AS roleId,\n" +
                "\tR.name ,\n" +
                "\tR.description,\n" +
                "\tTOTAL_COUNT.TOTAL_COUNT AS totalCount\n" +
                "FROM\n" +
                "\t(\n" +
                "\tSELECT\n" +
                "\t\t*\n" +
                "\tFROM\n" +
                "\t\troles R4\n" +
                "\tWHERE\n" +
                "\t\tR4.tenant_id = :tenantId\n" +
                "\t\tAND\n" +
                "\t\t(:roleName IS NULL\n" +
                "\t\t\tOR R4.name LIKE CONCAT('%',:roleName , '%'))) R\n" +
                "CROSS JOIN (\n" +
                "\tSELECT\n" +
                "\t\tCOUNT(*) AS TOTAL_COUNT\n" +
                "\tFROM\n" +
                "\t\troles R3\n" +
                "\tWHERE\n" +
                "\t\tR3.tenant_id = :tenantId\n" +
                "\t\tAND\n" +
                "\t\t(:roleName IS NULL\n" +
                "\t\t\tOR R3.name LIKE CONCAT('%',:roleName , '%'))) TOTAL_COUNT\n" +
                "ORDER BY R.ID DESC\n" +
                "LIMIT :limit OFFSET :offset"
)
@NamedNativeQuery(
        name = "Roles.getRoleDetails",
        resultSetMapping = "getRoleDetailsMapping",
        query = "select\n" +
                "\t CAST(r.id as CHAR) as roleId,\n" +
                "\tr.name as roleName ,\n" +
                "\tr.description,\n" +
                "\t CAST(m.id as CHAR) as menuId,\n" +
                "\tm.name as menuName,\n" +
                "\t CAST(c.id as CHAR) as componentId,\n" +
                "\tc.name as componentName,\n" +
                "\t CAST(p.id as CHAR)   as permissionId,\n" +
                "\tp.name as permissionName,\n" +
                "\tp.description as permissionDescription\n" +
                "from\n" +
                "\troles r\n" +
                "inner join role_to_permissions rtp on\n" +
                "\tr.id = rtp.role_id\n" +
                "\tand r.tenant_id = :tenantId and r.id = :roleId\n" +
                "\tleft join permission p on p.id = rtp.permission_id \n" +
                "left join components c on\n" +
                "\tc.id = p.component_id\n" +
                "left join menus m on\n" +
                "\tm.id = p.menu_id\n" +
                "ORDER BY r.id DESC"
)

@NamedNativeQuery(
        name = "Roles.getFilteredUserList",
        resultSetMapping = "findRoleListViewMapping",
        query = "SELECT\n" +
                "    CAST(R.ID AS CHAR) AS roleId,\n" +
                "    R.name,\n" +
                "    R.description,\n" +
                "    TOTAL_COUNT.TOTAL_COUNT AS totalCount\n" +
                "FROM (\n" +
                "    SELECT\n" +
                "        *\n" +
                "    FROM\n" +
                "        roles R4\n" +
                "    WHERE\n" +
                "        R4.tenant_id = :tenantId\n" +
                "        AND (\n" +
                "            (:roleId IS NULL AND :roleIdFilterType IS NULL) OR\n" +
                "            (:roleIdFilterType = 'Equal' AND CAST(R4.id AS CHAR) = :roleId) OR\n" +
                "            (:roleIdFilterType = 'Include' AND CAST(R4.id AS CHAR) LIKE CONCAT('%', :roleId, '%')) OR\n" +
                "            (:roleIdFilterType = 'Is empty' AND R4.id IS NULL)\n" +
                "        )\n" +
                "        AND (\n" +
                "            (:name IS NULL AND :nameFilterType IS NULL) OR\n" +
                "            (:nameFilterType = 'Equal' AND LOWER(R4.name) = LOWER(:name)) OR\n" +
                "            (:nameFilterType = 'Include' AND LOWER(R4.name) LIKE CONCAT('%', LOWER(:name), '%')) OR\n" +
                "            (:nameFilterType = 'Is empty' AND R4.name IS NULL)\n" +
                "        )\n" +
                "        AND (\n" +
                "            (:description IS NULL AND :descriptionFilterType IS NULL) OR\n" +
                "            (:descriptionFilterType = 'Equal' AND LOWER(R4.description) = LOWER(:description)) OR\n" +
                "            (:descriptionFilterType = 'Include' AND LOWER(R4.description) LIKE CONCAT('%', LOWER(:description), '%')) OR\n" +
                "            (:descriptionFilterType = 'Is empty' AND R4.description IS NULL)\n" +
                "        )\n" +
                ") R\n" +
                "CROSS JOIN (\n" +
                "    SELECT\n" +
                "        COUNT(*) AS TOTAL_COUNT\n" +
                "    FROM\n" +
                "        roles R3\n" +
                "    WHERE\n" +
                "        R3.tenant_id = :tenantId\n" +
                "        AND (\n" +
                "            (:roleId IS NULL AND :roleIdFilterType IS NULL) OR\n" +
                "            (:roleIdFilterType = 'Equal' AND CAST(R3.id AS CHAR) = :roleId) OR\n" +
                "            (:roleIdFilterType = 'Include' AND CAST(R3.id AS CHAR) LIKE CONCAT('%', :roleId, '%')) OR\n" +
                "            (:roleIdFilterType = 'Is empty' AND R3.id IS NULL)\n" +
                "        )\n" +
                "        AND (\n" +
                "            (:name IS NULL AND :nameFilterType IS NULL) OR\n" +
                "            (:nameFilterType = 'Equal' AND LOWER(R3.name) = LOWER(:name)) OR\n" +
                "            (:nameFilterType = 'Include' AND LOWER(R3.name) LIKE CONCAT('%', LOWER(:name), '%')) OR\n" +
                "            (:nameFilterType = 'Is empty' AND R3.name IS NULL)\n" +
                "        )\n" +
                "        AND (\n" +
                "            (:description IS NULL AND :descriptionFilterType IS NULL) OR\n" +
                "            (:descriptionFilterType = 'Equal' AND LOWER(R3.description) = LOWER(:description)) OR\n" +
                "            (:descriptionFilterType = 'Include' AND LOWER(R3.description) LIKE CONCAT('%', LOWER(:description), '%')) OR\n" +
                "            (:descriptionFilterType = 'Is empty' AND R3.description IS NULL)\n" +
                "        )\n" +
                ") TOTAL_COUNT\n" +
                "ORDER BY R.id DESC\n" +
                "LIMIT :limit OFFSET :offset"
)

public class Roles {
    @Id
    private Long id;
    private String name;
    private String description;

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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenants tenants;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "ROLE_TO_PERMISSIONS",
            joinColumns = @JoinColumn(name = "ROLE_ID"),
            inverseJoinColumns = @JoinColumn(name = "PERMISSION_ID"))
    private List<Permission> permissionList;

    @ManyToMany(mappedBy = "roles", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Users> usersList;


}

