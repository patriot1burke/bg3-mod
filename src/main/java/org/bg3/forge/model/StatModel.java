package org.bg3.forge.model;

import java.util.Map;

public record StatModel(String name, String type, String using, Map<String, String> data) {

}
