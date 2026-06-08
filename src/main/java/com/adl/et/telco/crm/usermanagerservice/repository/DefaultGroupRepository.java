package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.dto.common.MetaData;
import com.adl.et.telco.crm.usermanagerservice.model.metadata.DefaultGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DefaultGroupRepository extends JpaRepository<DefaultGroup, Long> {
    @Query(nativeQuery = true)
    List<MetaData> getDefaultGroupMetaData();
}
