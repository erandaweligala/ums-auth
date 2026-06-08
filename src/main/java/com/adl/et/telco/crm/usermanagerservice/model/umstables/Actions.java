package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import com.adl.et.telco.crm.usermanagerservice.dto.audit.ActionDetails;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_actions")
@SqlResultSetMapping(
        name = "getActionsMapping",
        classes = {
                @ConstructorResult(
                        targetClass = ActionDetails.class,
                        columns = {
                                @ColumnResult(name = "id", type = Long.class),
                                @ColumnResult(name = "name"),
                                @ColumnResult(name = "description")
                        }
                )
        }
)
@NamedNativeQuery(
        name = "Actions.getActions",
        resultSetMapping = "getActionsMapping",
        query = "select id , name, description from ums_actions a inner join ums_action_to_tenants att on a.id = att.action_id and att.tenant_id = :tenantId"
)
public class Actions {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    private String name;
    private String description;
    private boolean isMainAction;
    private String type;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPONENT_ID")
    private Components components;

    @ManyToMany(mappedBy = "actionsList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Permission> permissionList;

    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "ums_ACTION_TO_TENANTS",
            joinColumns = @JoinColumn(name = "ACTION_ID"),
            inverseJoinColumns = @JoinColumn(name = "TENANT_ID"))
    private List<Tenants> tenantsList;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "MAIN_ACTION_ID")
    private Actions mainAction;

}

