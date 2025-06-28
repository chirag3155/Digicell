package com.api.digicell.entities;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "assistant_options")
public class AssistantOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "assistant_id", nullable = false)
    private Integer assistantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "options_json", columnDefinition = "JSON")
    private String optionsJson;

    // Foreign key relationship (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_id", insertable = false, updatable = false)
    private AssistantConfiguration assistantConfiguration;

    // Default constructor
    public AssistantOptions() {}

    // Constructor with required fields
    public AssistantOptions(Integer assistantId, String name, String tenantId, List<String> options) {
        this.assistantId = assistantId;
        this.name = name;
        this.tenantId = tenantId;
        this.setOptionsFromList(options);
    }

    // Constructor without options (will use default)
    public AssistantOptions(Integer assistantId, String name, String tenantId) {
        this.assistantId = assistantId;
        this.name = name;
        this.tenantId = tenantId;
        this.setOptionsFromList(DefaultAssistantOptions.DEFAULT_OPTIONS);
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAssistantId() {
        return assistantId;
    }

    public void setAssistantId(Integer assistantId) {
        this.assistantId = assistantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    // Helper methods for JSON conversion
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> getOptionsAsList() {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public void setOptionsFromList(List<String> options) {
        try {
            this.optionsJson = objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            this.optionsJson = "[]";
        }
    }


    public AssistantConfiguration getAssistantConfiguration() {
        return assistantConfiguration;
    }

    public void setAssistantConfiguration(AssistantConfiguration assistantConfiguration) {
        this.assistantConfiguration = assistantConfiguration;
    }

    // Utility methods
    public boolean hasOption(String option) {
        List<String> options = getOptionsAsList();
        return options.contains(option);
    }

    public void addOption(String option) {
        List<String> options = getOptionsAsList();
        if (!options.contains(option)) {
            options = new ArrayList<>(options);
            options.add(option);
            setOptionsFromList(options);
        }
    }

    public void removeOption(String option) {
        List<String> options = getOptionsAsList();
        if (options.contains(option)) {
            options = new ArrayList<>(options);
            options.remove(option);
            setOptionsFromList(options);
        }
    }

    @Override
    public String toString() {
        return "AssistantOptions{" +
                "id=" + id +
                ", assistantId=" + assistantId +
                ", name='" + name + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", optionsCount=" + getOptionsAsList().size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssistantOptions)) return false;
        AssistantOptions that = (AssistantOptions) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

// Static utility class for default options
class DefaultAssistantOptions {
    public static final List<String> DEFAULT_OPTIONS = List.of(
        "Special Offers",
        "DTH offers", 
        "Digital apps",
        "Contact us",
        "Service plan and packages",
        "Bestselling mobile devices",
        "Smart Solutions",
        "Live Sports"
    );
} 