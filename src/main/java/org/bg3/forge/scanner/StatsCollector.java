package org.bg3.forge.scanner;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatsCollector {
    private static Pattern dataPattern = Pattern.compile("^data\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"$");
    private static Pattern newEntryPattern = Pattern.compile("^new entry\\s+\"([^\"]+)\"$");
    private static Pattern typePattern = Pattern.compile("^type\\s+\"([^\"]+)\"$");

    public static class Stat {
        public String name;
        public String type;
        public Map<String, String> data;

        public Stat(String name, String type, Map<String, String> data) {
            this.name = name;
            this.type = type;
            this.data = data;
        }
    }

    public static class Library extends HashMap<String, Map<String, Stat>> {

        public Set<String> collectAttributeValues(String type, String attribute) {
            return get(type).values().stream()
                .map(entry -> entry.data.get(attribute))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }
        public Set<String> collectAttributeNames(String type) {
            return get(type).values().stream()
                .map(entry -> entry.data.keySet())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        }

        public Set<String> collectAttributesValues(String attribute) {
            Set<String> attributes = new HashSet<>();
            for (String type : keySet()) {
                attributes.addAll(get(type).values().stream()
                    .map(entry -> entry.data.get(attribute))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
            }
            return attributes;
        }

        public Set<String> commonAttributes(String... types) {
            Set<String> commonAttributes = Arrays.stream(types).map(type -> get(type).values())
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

        public Library scan(Path path) throws IOException {
            AtomicReference<Stat> currentEntry = new AtomicReference<>();
            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(line -> {
                    Matcher newEntryMatcher = newEntryPattern.matcher(line);
                    if (newEntryMatcher.matches()) {
                        String name = newEntryMatcher.group(1);
                        currentEntry.set(new Stat(name, null, new HashMap<>()));
                        return;
                    }
                    Matcher typeMatcher = typePattern.matcher(line);
                    if (typeMatcher.matches()) {
                        String type = typeMatcher.group(1);
                        currentEntry.get().type = type;
                        Map<String, Stat> typeEntries = computeIfAbsent(type, k -> new HashMap<>());
                        typeEntries.put(currentEntry.get().name, currentEntry.get());
                        return;
                    }
                    Matcher matcher = dataPattern.matcher(line);
                    if (matcher.matches()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        currentEntry.get().data.put(key, value);
                        return;
                    }
                });
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
