package org.baldurs.forge.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.langchain4j.model.output.structured.Description;

public record EquipmentFilters(
    @JsonProperty("filters")
    @Description("List of equipment filters.  Must return a list even it only has one item.")
    List<EquipmentFilter> filters) {

    public static String toJson(EquipmentFilters equipmentFilters) {
        return EquipmentFilter.toJson(equipmentFilters.filters());
    }

}
