package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.dto.audit.ActionDetails;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Actions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionsRepository extends JpaRepository<Actions, Long> {

    @Query(nativeQuery = true)
    List<ActionDetails> getActions(long tenantId);
}

