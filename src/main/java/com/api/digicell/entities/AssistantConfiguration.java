package com.api.digicell.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity
@Table(name = "assistant_configuration", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssistantConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "role")
    private String role;

    @Column(name = "description")
    private String description;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "prompt")
    private String prompt;

    @Column(name = "created_on", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdOn;


    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedOn;


    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_dynamic_opening_message")
    private Boolean isDynamicOpeningMessage=false;

    @Column(name = "opening_message")
    private String openingMessage="Hi,how can I help you today?";

    @Column(name = "category")
    private String category;

    // @Column(name="assistant_type")
    // private String assistantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assistant_type", nullable = false)
    private AssistantType assistantType = AssistantType.normal;

    @Column(name = "opening_message_config")
    private String openingMessageConfig;

    // Auto-set timestamps before inserting or updating
    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now();
        this.updatedOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }


}

