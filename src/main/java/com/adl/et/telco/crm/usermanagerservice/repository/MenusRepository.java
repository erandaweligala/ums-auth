package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.model.umstables.Menus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenusRepository extends JpaRepository<Menus, Long> {

}

