package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.dto.permission.ActionHierarchyResultSet;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.PermissionDetailsResultSet;
import com.adl.et.telco.crm.usermanagerservice.dto.permission.PermissionView;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionsRepository extends JpaRepository<Permission, Long> {
    // Oracle: requires sequence UMS_UUID_SEQ to exist in the schema
    // (CREATE SEQUENCE ums_uuid_seq START WITH 1 INCREMENT BY 1 NOCACHE).
    @Query(value = "SELECT ums_uuid_seq.NEXTVAL FROM dual", nativeQuery = true)
    Long generateUuidShort();
    @Query(nativeQuery = true)
    List<PermissionView> getPermissionList(Long tenantId,String permissionName, Long menuId, Long componentId, int limit, int offset);
    @Query(nativeQuery = true)
    List<PermissionDetailsResultSet> findPermissionDetails(long tenantId, Long permissionId,Long componentId);
    @Query(nativeQuery = true)
    List<ActionHierarchyResultSet> getActionHierarchy(long tenantId, Long componantId);

    List<Permission> findAllByTenantId(long tenantId);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN EXISTS(select 1 from permission p where LOWER(p.name) = LOWER(:name) and p.menu_id = :menuId and p.component_id = :componentId and p.tenant_id = :tenantId ) THEN 1 ELSE 0 END FROM dual ")
    int isAlreadyExist(String name, Long menuId, Long componentId, Long tenantId);

    List<PermissionView> getFilteredPermissionList(String id, String idFilterType,
                                                   String name, String nameFilterType,
                                                   String description, String descriptionFilterType,
                                                   String componentName, String componentNameFilterType,
                                                   String menuName, String menuNameFilterType,
                                                   int limit, int offset, long tenantId);

    @Query("SELECT p FROM Permission p WHERE p.name = :name " +
            "AND p.menus.id = :menuId " +
            "AND p.components.id = :componentId " +
            "AND p.tenant.id = :tenantId")
    Optional<Permission> findByNameAndMenuIdAndComponentIdAndTenantId(
            @Param("name")        String name,
            @Param("menuId")      Long menuId,
            @Param("componentId") Long componentId,
            @Param("tenantId")    Long tenantId
    );

    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM permission", nativeQuery = true)
    Long getNextPermissionId();

}
