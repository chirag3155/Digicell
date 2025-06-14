package com.api.digicell.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_org_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"organization"}) // Ignore this field to break circular reference
public class UserOrgPermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Avoid proxy issues
    @JsonIgnore // Prevents Jackson from serializing the UserAccount reference
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Avoid proxy issues
    @JsonIgnore // Prevents Jackson from serializing the Organization reference
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Avoid proxy issues
    private ResourcePermissions resourcePermission;

    @Column(name = "access_label", nullable = true)
    private String accessLabel;
}