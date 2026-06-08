package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_attributes")
public class Attributes {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    private String name;
    private String description;
    private String path;
    private Boolean controllable;


    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ACTION_ID")
    private Actions actions;

    @ManyToMany(mappedBy = "attributesList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Permission> permissionList;

    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "ums_ATTRIBUTE_TO_TENANTS",
            joinColumns = @JoinColumn(name = "ATTRIBUTE_ID"),
            inverseJoinColumns = @JoinColumn(name = "TENANT_ID"))
    private List<Tenants> tenantsList;


}

