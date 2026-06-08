package com.adl.et.telco.crm.usermanagerservice.model.umstables;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "ums_action_log")
public class ActionLog {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    @Column(name = "activity_id",columnDefinition = "VARCHAR(128)")
    private String activityId;
    @Column(columnDefinition = "VARCHAR(255)")
    private String activity;

    @Column(columnDefinition = "VARCHAR(255)")
    private String status;

    @Column(name = "status_description")
    private String statusDescription;

    @Column(name = "user",columnDefinition = "VARCHAR(128)")
    private String user;

    @Column(name = "detailed_request",columnDefinition = "VARCHAR(255)")
    private String detailedRequest;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "url",columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(name = "action_id")
    private Integer actionId;

    @Column(name = "activity_type")
    private String activityType;


}

