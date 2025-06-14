package com.api.digicell.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "tenant_id", nullable = true)
    private String tenantId;

    @Column(name = "organization_name", nullable = false, length = 100)
    private String organizationName;

    @Column(name = "country_id", nullable = false)
    private int countryId;

    @Column(name = "hierarchy_level", nullable = false)
    private int hierarchyLevel;

    @Column(name = "poc_email", length = 100 , nullable = false)
    private String pocEmail;

    @Column(name = "organization_address",nullable = true)
    private String organizationAddress;

    @Column(name = "organization_desc",nullable = true)
    private String organizationDescription;

    @Column(name = "poc_name",nullable = false)
    private String pocName;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrganizationType organizationType;

    @Column(name = "parent_organization_id")
    private Long parentOrganizationId = -1L;

    @Column(name = "image_url",nullable = true)
    private String logoUrl;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserOrgPermissions> userOrgPermissions;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "created_on", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedOn;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdOn = now;
        updatedOn = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedOn = LocalDateTime.now();
    }
}