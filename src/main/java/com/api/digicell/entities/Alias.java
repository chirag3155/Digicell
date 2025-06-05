package com.api.digicell.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Represents an alias/shortcut that maps a short "key" to a longer "value" text which can be reused
 * in conversations (e.g. key = "wlc", value = "Welcome!").
 */
@Entity
@Table(name = "Aliases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alias_id")
    private Long aliasId;

    /** Short key for the alias – must be unique. */
    @NotBlank(message = "Alias key must not be blank")
    @Column(name = "alias_key", nullable = false, unique = true, length = 50)
    private String key;

    /** Full value/text that the alias expands to. */
    @NotBlank(message = "Alias value must not be blank")
    @Column(name = "alias_value", nullable = false, columnDefinition = "TEXT")
    private String value;

} 