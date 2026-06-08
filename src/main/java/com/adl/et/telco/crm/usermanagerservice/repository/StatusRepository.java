package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.model.umstables.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    @Query(nativeQuery = true)
    List<MetaData> getUserStatus();
}

