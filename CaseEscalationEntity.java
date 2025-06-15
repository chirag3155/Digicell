package com.getinsured.cp.escalation.adapters.repository.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "tenant_1",name = "case_escalation")
public class CaseEscalationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_escalation_id", nullable = false)
    private Integer caseEscalationId;

    @Column(name = "escalation_id", nullable = false)
    private Integer escalationId;

    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;

    @Column(name = "case_detail_id")
    private Integer caseDetailId;

    @Column(name = "cp_user_id")
    private Integer cpUserId;

    @Column(name = "escalation_deadline")
    private LocalDate escalationDeadline;

    @Column(name = "escalation_notes", nullable = false, length = 1000)
    private String escalationNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "case_detail_id", referencedColumnName = "case_detail_id", insertable = false, updatable = false),
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false)
    })
    private CaseDetailEntity caseDetail;
} 