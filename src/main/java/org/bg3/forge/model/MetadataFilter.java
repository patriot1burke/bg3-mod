package org.bg3.forge.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public record MetadataFilter(String type, String slot, String name) {
    public static List<MetadataFilter> fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<MetadataFilter>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(List<MetadataFilter> metadataFilters) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
            return mapper.writeValueAsString(metadataFilters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
