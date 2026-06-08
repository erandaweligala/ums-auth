package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import com.adl.et.telco.crm.usermanagerservice.dto.permission.ActionHierarchyResultSet;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.PermissionDetailsResultSet;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.PermissionView;
import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SqlResultSetMapping(
        name = "getPermissionListMapping",
        classes = {
                @ConstructorResult(
                        targetClass = PermissionView.class,
                        columns = {
                                @ColumnResult(name = "permissionId"),
                                @ColumnResult(name = "name"),
                                @ColumnResult(name = "description"),
                                @ColumnResult(name = "componentname"),
                                @ColumnResult(name = "menuname"),
                                @ColumnResult(name = "totalCount", type = Integer.class)
                        }
                )
        }
)
@SqlResultSetMapping(
        name = "findPermissionDetailsMapping",
        classes = {
                @ConstructorResult(
                        targetClass = PermissionDetailsResultSet.class,
                        columns = {
                                @ColumnResult(name = "attribute_id", type = Long.class),
                                @ColumnResult(name = "is_main_action", type = Integer.class),
                                @ColumnResult(name = "attribute_is_selected", type = Integer.class),


                                @ColumnResult(name = "action_id", type = Long.class),
                                @ColumnResult(name = "attribute_name"),
                                @ColumnResult(name = "action_is_selected", type = Integer.class),

                                @ColumnResult(name = "main_action_id", type = Long.class),
                                @ColumnResult(name = "action_name")
                        }
                )
        }
)
@SqlResultSetMapping(
        name = "getActionHierarchyMapping",
        classes = {
                @ConstructorResult(
                        targetClass = ActionHierarchyResultSet.class,
                        columns = {
                                @ColumnResult(name = "attribute_id", type = Long.class),
                                @ColumnResult(name = "is_main_action", type = Integer.class),
                                @ColumnResult(name = "action_id", type = Long.class),
                                @ColumnResult(name = "attribute_name"),
                                @ColumnResult(name = "main_action_id", type = Long.class),
                                @ColumnResult(name = "action_name")
                        }
                )
        }
)


@NamedNativeQuery(
        name = "Permission.getPermissionList",
        resultSetMapping = "getPermissionListMapping",
        query = "select\n" +
                "\tTO_CHAR(t.permissionId) as permissionId ,\n" +
                "\tt.name,\n" +
                "\tt.description,\n" +
                "\tt.componentname ,\n" +
                "\tt .menuname,\n" +
                "\ttotal_count.totalCount\n" +
                "from\n" +
                "\t(\n" +
                "\tselect\n" +
                "\t\tp.id as permissionId,\n" +
                "\t\tp.name,\n" +
                "\t\tp.description,\n" +
                "\t\tc.name as componentName,\n" +
                "\t\tm .name as menuName\n" +
                "\tfrom\n" +
                "\t\t(\n" +
                "\t\tselect\n" +
                "\t\t\tp1.id,\n" +
                "\t\t\tp1.name,\n" +
                "\t\t\tp1.description,\n" +
                "\t\t\tp1.component_id\n" +
                "\t\tfrom\n" +
                "\t\t\tpermission p1\n" +
                "\t\twhere\n" +
                "\t\t\tp1.tenant_id = :tenantId\n" +
                "\t\t\tand ( :menuId is null\n" +
                "\t\t\t\tor p1.menu_id = :menuId )\n" +
                "\t\t\tand (:componentId is null\n" +
                "\t\t\t\tor p1.component_id = :componentId )\n" +
                "\t\t\tand ( :permissionName is null\n" +
                "\t\t\t\tor p1.name like '%' || :permissionName || '%' )) p\n" +
                "\tleft join components c\n" +
                "                      on\n" +
                "\t\tp.component_id = c.id\n" +
                "\t\tand ( :componentId is null\n" +
                "\t\t\tor c.id = :componentId )\n" +
                "\tleft join menus m\n" +
                "                      on\n" +
                "\t\tm.id = c.menu_id) t\n" +
                "cross join (\n" +
                "\tselect\n" +
                "\t\tCount(p.id) as totalCount\n" +
                "\tfrom\n" +
                "\t\t(\n" +
                "\t\tselect\n" +
                "\t\t\tp1.id,\n" +
                "\t\t\tp1.name,\n" +
                "\t\t\tp1.description,\n" +
                "\t\t\tp1.component_id\n" +
                "\t\tfrom\n" +
                "\t\t\tpermission p1\n" +
                "\t\twhere\n" +
                "\t\t\tp1.tenant_id = :tenantId\n" +
                "\t\t\tand ( :menuId is null\n" +
                "\t\t\t\tor p1.menu_id = :menuId )\n" +
                "\t\t\t\tand (:componentId is null\n" +
                "\t\t\t\tor p1.component_id = :componentId )\n" +
                "\t\t\tand ( :permissionName is null\n" +
                "\t\t\t\tor p1.name like '%' || :permissionName || '%' )) p\n" +
                "\tleft join components c\n" +
                "                                on\n" +
                "\t\tp.component_id = c.id\n" +
                "\t\tand ( :componentId is null\n" +
                "\t\t\tor c.id = :componentId )\n" +
                "\tleft join menus m\n" +
                "                                on\n" +
                "\t\tm.id = c.menu_id) total_count\n" +
                "ORDER BY t.permissionId DESC\n" +
                "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY"
)
@NamedNativeQuery(
        name = "Permission.findPermissionDetails",
        resultSetMapping = "findPermissionDetailsMapping",
        query = "select\n" +
                "\ta.id as action_id,\n" +
                "\ta.is_main_action ,\n" +
                "\ta.main_action_id ,a.name as action_name ,\n" +
                "\tCASE WHEN pta.action_id is not null\n" +
                "\tTHEN 1\n" +
                "\tELSE 0 END as action_is_selected,\n" +
                "\ta2.id as attribute_id,\n" +
                "\ta2.name as attribute_name,\n" +
                "\tCASE WHEN pta2.attribute_id  is not null\n" +
                "\tTHEN 1\n" +
                "\tELSE 0 END as attribute_is_selected\n" +
                "from\n" +
                "\tpermission p\n" +
                "inner join components c on\n" +
                "\tc.menu_id = p.menu_id\n" +
                "\tand c.id = :componentId\n" +
                "\tand p.id = :permissionId\n" +
                "\tand p.tenant_id = :tenantId\n" +
                "left join actions a on\n" +
                "\ta.component_id = c.id\n" +
                "left join permission_to_actions pta on\n" +
                "\tpta.permission_id = p.id\n" +
                "\tand pta.action_id = a.id\n" +
                "left join attributes a2 on\n" +
                "\ta2.action_id = a.id\n" +
                "left join permission_to_attributes pta2 on\n" +
                "\ta2.id = pta2.attribute_id\n" +
                "\tand pta2.permission_id = p.id"
)
@NamedNativeQuery(
        name = "Permission.getActionHierarchy",
        resultSetMapping = "getActionHierarchyMapping",
        query = "\tselect\n" +
                "\ta.id as action_id,\n" +
                "\ta.is_main_action ,\n" +
                "\ta.main_action_id ,\n" +
                "\ta.name as action_name ,\n" +
                "\ta2.id as attribute_id,\n" +
                "\ta2.name as attribute_name\n" +
                "from\n" +
                "\tactions a\n" +
                "inner join action_to_tenants att on\n" +
                "\ta.id = att.action_id\n" +
                "\tand att.tenant_id = :tenantId\n" +
                "\tand a.component_id = :componantId\n" +
                "left join attributes a2 on\n" +
                "\ta2.action_id = a.id "
)

@SqlResultSetMapping(
        name = "getFilteredPermissionListMapping",
        classes = {
                @ConstructorResult(
                        targetClass = PermissionView.class,
                        columns = {
                                @ColumnResult(name = "permissionId", type = String.class),
                                @ColumnResult(name = "name", type = String.class),
                                @ColumnResult(name = "description", type = String.class),
                                @ColumnResult(name = "componentName", type = String.class),
                                @ColumnResult(name = "menuName", type = String.class),
                                @ColumnResult(name = "totalCount", type = Integer.class)
                        }
                )
        }
)

@NamedNativeQuery(
        name = "Permission.getFilteredPermissionList",
        resultSetMapping = "getFilteredPermissionListMapping",
        query = "SELECT\n" +
                "    TO_CHAR(p.id) AS permissionId,\n" +
                "    p.name,\n" +
                "    p.description,\n" +
                "    c.name AS componentName,\n" +
                "    m.name AS menuName,\n" +
                "    (SELECT COUNT(*) FROM permission p2 \n" +
                "     WHERE p2.tenant_id = :tenantId\n" +
                "     AND (\n" +
                "        :id IS NULL OR\n" +
                "        (:idFilterType = 'Equal' AND p.id = :id)\n" +
                "    )\n" +
                "    AND (\n" +
                "        :name IS NULL OR\n" +
                "        (:nameFilterType = 'Equal' AND LOWER(p.name) = LOWER(:name)) OR\n" +
                "        (:nameFilterType = 'Include' AND LOWER(p.name) LIKE '%' || LOWER(:name) || '%') OR\n" +
                "        (:nameFilterType = 'Is empty' AND (p.name IS NULL OR p.name = ''))\n" +
                "    )\n" +
                "    AND (\n" +
                "        :description IS NULL OR\n" +
                "        (:descriptionFilterType = 'Equal' AND LOWER(p.description) = LOWER(:description)) OR\n" +
                "        (:descriptionFilterType = 'Include' AND LOWER(p.description) LIKE '%' || LOWER(:description) || '%') OR\n" +
                "        (:descriptionFilterType = 'Is empty' AND (p.description IS NULL OR p.description = ''))\n" +
                "    )\n" +
                "    AND (\n" +
                "        :componentName IS NULL OR\n" +
                "        (:componentNameFilterType = 'Equal' AND LOWER(c.name) = LOWER(:componentName)) OR\n" +
                "        (:componentNameFilterType = 'Include' AND LOWER(c.name) LIKE '%' || LOWER(:componentName) || '%') OR\n" +
                "        (:componentNameFilterType = 'Is empty' AND (c.name IS NULL OR c.name = ''))\n" +
                "    )\n" +
                "    AND (\n" +
                "        :menuName IS NULL OR\n" +
                "        (:menuNameFilterType = 'Equal' AND LOWER(m.name) = LOWER(:menuName)) OR\n" +
                "        (:menuNameFilterType = 'Include' AND LOWER(m.name) LIKE '%' || LOWER(:menuName) || '%') OR\n" +
                "        (:menuNameFilterType = 'Is empty' AND (m.name IS NULL OR m.name = ''))\n" +
                "    )) AS totalCount\n" +
                "FROM\n" +
                "    permission p\n" +
                "LEFT JOIN components c ON c.id = p.component_id\n" +
                "LEFT JOIN menus m ON m.id = p.menu_id\n" +
                "WHERE\n" +
                "    p.tenant_id = :tenantId\n" +
                "    AND (\n" +
                "        :id IS NULL OR\n" +
                "        (:idFilterType = 'Equal' AND p.id = :id)\n" +
                "    )\n" +
                "    AND (\n" +
                "        :name IS NULL OR\n" +
                "        (:nameFilterType = 'Equal' AND LOWER(p.name) = LOWER(:name)) OR\n" +
                "        (:nameFilterType = 'Include' AND LOWER(p.name) LIKE '%' || LOWER(:name) || '%') OR\n" +
                "        (:nameFilterType = 'Is empty' AND (p.name IS NULL OR p.name = ''))\n" +
                "    )\n" +
                "    AND (\n" +
                "        :description IS NULL OR\n" +
                "        (:descriptionFilterType = 'Equal' AND LOWER(p.description) = LOWER(:description)) OR\n" +
                "        (:descriptionFilterType = 'Include' AND LOWER(p.description) LIKE '%' || LOWER(:description) || '%') OR\n" +
                "        (:descriptionFilterType = 'Is empty' AND (p.description IS NULL OR p.description = ''))\n" +
                "    )\n" +
                "    AND (\n" +
                "        :componentName IS NULL OR\n" +
                "        (:componentNameFilterType = 'Equal' AND LOWER(c.name) = LOWER(:componentName)) OR\n" +
                "        (:componentNameFilterType = 'Include' AND LOWER(c.name) LIKE '%' || LOWER(:componentName) || '%') OR\n" +
                "        (:componentNameFilterType = 'Is empty' AND (c.name IS NULL OR c.name = ''))\n" +
                "    )\n" +
                "    AND (\n" +
                "        :menuName IS NULL OR\n" +
                "        (:menuNameFilterType = 'Equal' AND LOWER(m.name) = LOWER(:menuName)) OR\n" +
                "        (:menuNameFilterType = 'Include' AND LOWER(m.name) LIKE '%' || LOWER(:menuName) || '%') OR\n" +
                "        (:menuNameFilterType = 'Is empty' AND (m.name IS NULL OR m.name = ''))\n" +
                "    )\n" +
                "ORDER BY p.id DESC\n" +
                "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY"
)

public class Permission {
    @Id
    private Long id;
    private String name;
    private String type;
    private String description;

    @ManyToMany(mappedBy = "permissionList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Roles> rolesList;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "PERMISSION_TO_ACTIONS",
            joinColumns = @JoinColumn(name = "PERMISSION_ID"),
            inverseJoinColumns = @JoinColumn(name = "ACTION_ID"))
    private List<Actions> actionsList;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "PERMISSION_TO_ATTRIBUTES",
            joinColumns = @JoinColumn(name = "PERMISSION_ID"),
            inverseJoinColumns = @JoinColumn(name = "ATTRIBUTE_ID"))
    private List<Attributes> attributesList;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "MENU_ID")
    private Menus menus;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPONENT_ID")
    private Components components;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "STATUS_ID")
    private Status status;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID")
    private Tenants tenant;
}

