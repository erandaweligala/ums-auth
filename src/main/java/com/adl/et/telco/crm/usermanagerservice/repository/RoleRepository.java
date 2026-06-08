package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleDetailsRepoDTO;
import com.adl.et.telco.crm.usermanagerservice.dto.role.RoleView;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Long> {

    @Query(value = "SELECT UUID_SHORT()", nativeQuery = true)
    Long generateUuidShort();

    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM roles", nativeQuery = true)
    Long getNextRoleId();

    @Query(nativeQuery = true)
    List<MetaData> findRoleListMetaData(Long tenantId);

    @Query(nativeQuery = true)
    List<RoleView> findRoleListView(long tenantId, int limit, int offset, String roleName);

    @Query(nativeQuery = true)
    List<RoleDetailsRepoDTO> getRoleDetails(long roleId, long tenantId);

    @Query(nativeQuery = true, value = "select * from roles r where r.id=:roleId and r.tenant_id=:tenantId")
    Optional<Roles> findByIdAndTenantId(Long roleId, long tenantId);

    @Query(nativeQuery = true)
    List<RoleView> getFilteredUserList(String roleId, String roleIdFilterType,
                                       String name, String nameFilterType,
                                       String description, String descriptionFilterType, int limit, int offset, long tenantId);

    @Query(nativeQuery = true, value = "select count(*) from roles r where lower(r.name) = lower(:roleName) and r.tenant_id=:tenantId")
    long isNameAlreadyExist(String roleName, long tenantId);

    Optional<Roles> findByNameAndTenantsId(String name, Long tenantId);
}

