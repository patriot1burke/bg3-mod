package org.baldurs.forge.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.model.output.structured.Description;

public record EquipmentFilter(
 
    @JsonProperty(required = false)
    @Description("""
            The type of equipment to filter for.  
            If the user is asking about any kind of weapon, the value Weapon should be used.
            If the user is asking about any kind of armor, the value Armor should be used.
            """)
    EquipmentType type, 
    
    @Description("If the type is a weapon that shoots, then the slot value is Ranged.")
    @JsonProperty(required = false)
    EquipmentSlot slot,

    @JsonProperty(required = false)
    ArmorClassFilter armorClass) {
    public static List<EquipmentFilter> fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<EquipmentFilter>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(List<EquipmentFilter> metadataFilters) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
            return mapper.writeValueAsString(metadataFilters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }



}
