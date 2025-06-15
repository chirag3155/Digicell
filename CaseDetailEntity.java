package com.getinsured.cp.escalation.adapters.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "case_detail", schema = "tenant_1", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "case_detail_id"}))
public class CaseDetailEntity extends BaseEntity {

    @Column(name = "tenant_id")
    private Integer tenantId;

    @Id
    @Column(name = "case_detail_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer caseDetailId;  // Fixed field name

    @Column(name = "sor_case_id")
    private String sorCaseId;

    @Column(name = "case_name")
    private String caseName;

    @Column(name = "date_opened")
    private LocalDate dateOpened;

    @Column(name = "last_huddle_date")
    private LocalDate lastHuddleDate;

    @Column(name = "next_huddle_date")
    private LocalDate nextHuddleDate;

    @Column(name = "sor_worker_id")
    private String sorWorkerId;

    @Column(name = "worker_id")
    private Integer workerId;

    @Column(name = "case_status_id")
    private Integer caseStatusId;

    @Column(name = "sor_case_status")
    private String sorCaseStatus;

    @Column(name = "sor_local_consult_count")
    private Integer sorLocalConsultCount;

    @Column(name = "sor_central_consult_count")
    private Integer sorCentralConsultCount;

    @Column(name = "days_open_id")
    private Integer daysOpenId;

    @Column(name = "sor_area_id")
    private Integer sorAreaId;
} 