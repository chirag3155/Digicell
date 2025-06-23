package com.api.digicell.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assistant_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "age")
    private Integer age;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "assistant_type", nullable = false)
    private AssistantType assistantType = AssistantType.NORMAL;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "is_dynamic_opening_message", nullable = false)
    private Boolean isDynamicOpeningMessage;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "opening_message", nullable = false)
    private String openingMessage;
    
    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;
    
    @Column(name = "role")
    private String role;
    
    @Column(name = "role_id", nullable = false)
    private Integer roleId;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    @UpdateTimestamp
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
    
    @Column(name = "opening_message_config")
    private String openingMessageConfig;
    
    public enum AssistantType {
        NORMAL, MULTIAGENT
    }
} 