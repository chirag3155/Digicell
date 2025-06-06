package com.api.digicell.mapper;

import com.api.digicell.dto.AliasCreateDTO;
import com.api.digicell.dto.AliasResponseDTO;
import com.api.digicell.dto.AliasUpdateDTO;
import com.api.digicell.entities.Alias;
import org.springframework.stereotype.Component;

@Component
public class AliasMapper {
    
    public AliasResponseDTO toResponseDTO(Alias alias) {
        if (alias == null) {
            return null;
        }
        return new AliasResponseDTO(alias.getKey(), alias.getValue());
    }
    
    public Alias toEntity(AliasCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Alias alias = new Alias();
        alias.setKey(dto.getKey());
        alias.setValue(dto.getValue());
        return alias;
    }
    
    public void updateEntity(Alias alias, AliasUpdateDTO dto) {
        if (alias == null || dto == null) {
            return;
        }
        alias.setValue(dto.getValue());
    }
} 