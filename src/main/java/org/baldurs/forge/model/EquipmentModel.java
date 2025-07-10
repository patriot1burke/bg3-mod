package org.baldurs.forge.model;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public record EquipmentModel(
                String id,
                EquipmentType type,
                EquipmentSlot slot,
                Rarity rarity,
                String name,
                String description,
                String boostDescription,
                int armorClass,
                String weaponType,
                String armorType,
                Set<String> weaponProperties,
                String icon) {

        public static String toJson(List<EquipmentModel> models) {
                try {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                        return mapper.writeValueAsString(models);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }
        }

        public static EquipmentModel from(Equipment equipment) {
                return new EquipmentModel(equipment.id(), equipment.type(), equipment.slot(), equipment.rarity(), equipment.name(), equipment.description(), equipment.boostDescription(), equipment.armorClass(), equipment.weaponType(), equipment.armorType(), equipment.weaponProperties(), equipment.icon());
        }

}
