package org.baldurs.forge.scanner;

public record Handle(String id, String version) {

    public static Handle fromString(String handle) {
        String version = "*";
        String id = handle;
        int index = handle.indexOf(";");
        if (index >= 0) {
            id = handle.substring(0, index);
            version = handle.substring(index + 1);
        }
        return new Handle(id, version);
    }
}
