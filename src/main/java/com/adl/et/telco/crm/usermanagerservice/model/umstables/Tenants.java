package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Tenants {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    private String name;

    @ManyToMany(mappedBy = "tenantsList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Menus> menusList;

    @ManyToMany(mappedBy = "tenantsList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Components> componentsList;

    @ManyToMany(mappedBy = "tenantsList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Actions> actionsList;

    @ManyToMany(mappedBy = "tenantsList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attributes> attributesList;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private List<Permission> permissionList;
}

