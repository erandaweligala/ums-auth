package com.adl.et.telco.crm.usermanagerservice.repository;

import com.adl.et.telco.crm.usermanagerservice.model.umstables.ActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface UserActionLogRepository extends JpaRepository<ActionLog, Long> {
    @Modifying
    @Query("update ActionLog u set u.status=:status ,u.statusDescription=:statusDescription where u.activityId=:activityId")
    void updateStatus(@Param(value="activityId")String activityId, @Param(value = "status")String status, @Param(value = "statusDescription")String statusDescription);
}
