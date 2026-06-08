package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_components")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Components {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    private String name;
    private String description;

    @OneToMany(mappedBy = "components" , cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private List<Actions> actionsList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MENU_ID")
    private Menus menus;

    @OneToMany(mappedBy = "components", fetch = FetchType.LAZY)
    private List<Permission> permissionList;

    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "ums_COMPONENT_TO_TENANTS",
            joinColumns = @JoinColumn(name = "COMPONENT_ID"),
            inverseJoinColumns = @JoinColumn(name = "TENANT_ID"))
    private List<Tenants> tenantsList;
}

