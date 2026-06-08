package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.model.umstables.Attributes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributeRepository extends JpaRepository<Attributes, Long> {

    @Query(value = "select * from ums_attributes a where a.action_id in (:actions) and a.id in (select att.attribute_id from ums_attribute_to_tenants att where att.tenant_id =  :tenantId )  ",nativeQuery = true)
    List<Attributes> getControllableAttributes(List<Long> actions, Long tenantId);

    @Query(value = "select * from ums_attributes a where a.id in (select att.attribute_id from ums_attribute_to_tenants att where att.tenant_id =  :tenantId )  ",nativeQuery = true)
    List<Attributes> getAttributesDetails(long tenantId);
}

