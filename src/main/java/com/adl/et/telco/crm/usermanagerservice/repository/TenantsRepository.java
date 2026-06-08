package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.model.umstables.Tenants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantsRepository extends JpaRepository<Tenants, Long> {
}

