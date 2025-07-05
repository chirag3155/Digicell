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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import com.fasterxml.jackson.core.type.TypeReference;

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

    /**
     * Get options as a List (backward compatibility)
     * This method now returns keys from the Map for consistency
     */
    public List<String> getOptionsAsList() {
        Map<String, String> optionsMap = getOptionsAsMap();
        return new ArrayList<>(optionsMap.keySet());
    }

    /**
     * Get options as a Map (key-value pairs) directly from the database JSON
     * This method tries to parse the JSON as an object first, then falls back to array format
     */
    public Map<String, String> getOptionsAsMap() {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        
        try {
            // Try to parse as Map first (new object format from DB)
            return objectMapper.readValue(optionsJson, 
                new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (JsonProcessingException e) {
            // Fallback: Parse as array and convert keys to Map (backward compatibility)
            try {
                List<String> optionsList = objectMapper.readValue(optionsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                // Convert list to map with keys as both key and value for backward compatibility
                Map<String, String> optionsMap = new LinkedHashMap<>();
                for (String option : optionsList) {
                    optionsMap.put(option, option);
                }
                return optionsMap;
            } catch (JsonProcessingException ex) {
                return new LinkedHashMap<>();
            }
        }
    }

    public void setOptionsFromList(List<String> options) {
        try {
            this.optionsJson = objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            this.optionsJson = "[]";
        }
    }

    /**
     * Set options from a Map (key-value pairs)
     */
    public void setOptionsFromMap(Map<String, String> optionsMap) {
        try {
            this.optionsJson = objectMapper.writeValueAsString(optionsMap);
        } catch (JsonProcessingException e) {
            this.optionsJson = "{}";
        }
    }


    public AssistantConfiguration getAssistantConfiguration() {
        return assistantConfiguration;
    }

    public void setAssistantConfiguration(AssistantConfiguration assistantConfiguration) {
        this.assistantConfiguration = assistantConfiguration;
    }

    // Utility methods - Updated to work with Map format
    public boolean hasOption(String option) {
        Map<String, String> options = getOptionsAsMap();
        return options.containsKey(option);
    }

    public void addOption(String option, String question) {
        Map<String, String> options = getOptionsAsMap();
        options.put(option, question);
        setOptionsFromMap(options);
    }

    public void removeOption(String option) {
        Map<String, String> options = getOptionsAsMap();
        if (options.containsKey(option)) {
            options.remove(option);
            setOptionsFromMap(options);
        }
    }

    // Backward compatibility methods
    public void addOption(String option) {
        addOption(option, "Tell me about " + option.toLowerCase() + "?");
    }

    public String getQuestionForOption(String option) {
        Map<String, String> options = getOptionsAsMap();
        return options.get(option);
    }

    @Override
    public String toString() {
        return "AssistantOptions{" +
                "id=" + id +
                ", assistantId=" + assistantId +
                ", name='" + name + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", optionsCount=" + getOptionsAsMap().size() +
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