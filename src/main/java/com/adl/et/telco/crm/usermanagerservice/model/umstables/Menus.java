package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_menus")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Menus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String displayName;
    private Long parentId;

    @OneToMany(mappedBy = "menus", fetch = FetchType.LAZY)
    private List<Permission> permissionList;

    @OneToMany(mappedBy = "menus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Components> componentsList;

    @ManyToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "ums_MENU_TO_TENANTS",
            joinColumns = @JoinColumn(name = "MENU_ID"),
            inverseJoinColumns = @JoinColumn(name = "TENANT_ID"))
    private List<Tenants> tenantsList;
}

