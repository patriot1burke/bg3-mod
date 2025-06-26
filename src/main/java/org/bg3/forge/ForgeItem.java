package org.bg3.forge;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.Column;

public class ForgeItem {

    @Column(length = 255)
    public String name;
    
    @Column(length = 4000)
    public String description;
    
    @Column(length = 100)
    public String type;
    
    @Column(length = 50)
    public String id;
    
    @Column(length = 1000)
    public String weaponAttributes;
    
    @Column(length = 100)
    public String baseDamage;
    
    @Column(length = 100)
    public String slot;
    

    public static List<ForgeItem> loadJson() throws Exception {
        ClassLoader classLoader = ForgeItem.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("items.json")) {
            if (inputStream == null) {
                throw new Exception("File does not exist in classpath: items.json");
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            List<ForgeItem> items = mapper.readValue(json, new TypeReference<List<ForgeItem>>() {});
            return items;
        }
    }

    public static ForgeItem toForgeItem(Item item) {
        ForgeItem forgeItem = new ForgeItem();
        forgeItem.name = item.title;
        forgeItem.description = item.description;
        return forgeItem;
    }

    public static String toJson(List<ForgeItem> items) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            return mapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
        

}
