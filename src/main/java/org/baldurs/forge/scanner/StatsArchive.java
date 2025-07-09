package org.baldurs.forge.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

public class StatsArchive {
    private static Pattern dataPattern = Pattern.compile("^data\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"$");
    private static Pattern newEntryPattern = Pattern.compile("^new entry\\s+\"([^\"]+)\"$");
    private static Pattern typePattern = Pattern.compile("^type\\s+\"([^\"]+)\"$");
    private static Pattern usingPattern = Pattern.compile("^using\\s+\"([^\"]+)\"$");

    public static class Stat {
        public String name;
        public String type;
        public String using;
        @JsonIgnore
        public Library library;
        public Map<String, String> data;

        public Stat() {
        }

        public Stat(String name, String type, Library library, Map<String, String> data) {
            this.name = name;
            this.type = type;
            this.library = library;
            this.data = data;
        }
        public String getField(String field) {
            String val = data.get(field);
            if (val == null) {
                Stat parent = library.getByName(using);
                if (parent != null) {
                    val = parent.getField(field);
                }
            }
            return val;
        }

        public Map<String, String> aggregateData() {
            Map<String, String> data = new HashMap<>();
            if (using != null) {
                Stat parent = library.getByName(using);
                if (parent != null) {
                    data.putAll(parent.aggregateData());
                }
            }
            data.putAll(this.data);
            return data;
        }
     }

    public static class Library {
        public static final String ARMOR_TYPE = "Armor";
        public static final String WEAPON_TYPE = "Weapon";
        private Map<String, Stat> byName = new HashMap<>();
        private Map<String, Map<String, Stat>> byType = new HashMap<>();

        public Map<String, Stat> getArmor() {
            return byType.get(ARMOR_TYPE);
        }

        public Map<String, Stat> getWeapons() {
            return byType.get(WEAPON_TYPE);
        }

        public Stat getByName(String name) {
            return byName.get(name);
        }

        public Set<String> collectAttributeValues(String type, String attribute) {
            return byType.get(type).values().stream()
                .map(entry -> entry.data.get(attribute))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }
        public Set<String> collectAttributeNames(String type) {
            return byType.get(type).values().stream()
                .map(entry -> entry.data.keySet())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        }

        public Set<String> collectAttributesValues(String attribute) {
            Set<String> attributes = new HashSet<>();
            for (String type : byType.keySet()) {
                attributes.addAll(byType.get(type).values().stream()
                    .map(entry -> entry.data.get(attribute))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
            }
            return attributes;
        }

        public Set<String> commonAttributes(String... types) {
            Set<String> commonAttributes = Arrays.stream(types).map(type -> byType.get(type).values())
                                             .flatMap(Collection::stream)
                                             .map(entry -> entry.data.keySet())
                                             .flatMap(Collection::stream)
                                             .collect(Collectors.toSet());
            for (String type : types) {
                Set<String> attributes = collectAttributeNames(type);
                commonAttributes.retainAll(attributes);
            }
            return commonAttributes;
        }

        public void save(Path dest) throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(dest.toFile(), byName);
        }

        public void load(Path dest) throws Exception {
            ObjectMapper objectMapper = new ObjectMapper();
            byName = objectMapper.readValue(dest.toFile(), new TypeReference<Map<String, Stat>>() {});
            for (Stat stat : byName.values()) {
                byType.computeIfAbsent(stat.type, k -> new HashMap<>()).put(stat.name, stat);
                stat.library = this;
            }
            Log.info("Loaded " + byName.size() + " stats");
        }

        public Library scan(Path path) throws IOException {
            if (!Files.exists(path)) {
                Log.infof("Cannot scan statsfile %s, it does not exist", path);
                return this;
            }
            if (Files.isDirectory(path)) {
                for (Path file : Files.list(path).toList()) {
                    scanFile(file);
                }
            } else {
                scanFile(path);
            }
            return this;
        }

        private Library scanFile(Path path) throws IOException {
            AtomicReference<Stat> currentEntry = new AtomicReference<>();
            AtomicInteger sum = new AtomicInteger(0);
            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(line -> {
                    Matcher matcher = dataPattern.matcher(line);
                    if (matcher.matches()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        currentEntry.get().data.put(key, value);
                        return;
                    }
                    Matcher newEntryMatcher = newEntryPattern.matcher(line);
                    if (newEntryMatcher.matches()) {
                        String name = newEntryMatcher.group(1);
                        currentEntry.set(new Stat(name, null, this, new HashMap<>()));
                        return;
                    }
                    Matcher typeMatcher = typePattern.matcher(line);
                    if (typeMatcher.matches()) {
                        String type = typeMatcher.group(1);
                        currentEntry.get().type = type;
                        Map<String, Stat> typeEntries = byType.computeIfAbsent(type, k -> new HashMap<>());
                        typeEntries.put(currentEntry.get().name, currentEntry.get());
                        byName.put(currentEntry.get().name, currentEntry.get());
                        sum.incrementAndGet();
                        return;
                    }
                    Matcher usingMatcher = usingPattern.matcher(line);
                    if (usingMatcher.matches()) {
                        String using = usingMatcher.group(1);
                        currentEntry.get().using = using;
                        return;
                    }
                });
                Log.info("Scanned " + sum.get() + " stats from " + path);
                return this;
            }
        }
     }

     public static Library scan(Path path) throws IOException {
        Library library = new Library();
        library.scan(path);
        return library;
     } 
 

   
    
}
