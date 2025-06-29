package org.bg3.forge.model;

import dev.langchain4j.model.output.structured.Description;

public record ArmorClass(@Description("A comparator for the value field") Operator operator, @Description("The operand of the comparison defined in operator field") String value) {

}
