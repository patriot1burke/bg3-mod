package org.baldurs.forge.model;

import dev.langchain4j.model.output.structured.Description;

public record ArmorClassFilter(@Description("A comparator for the value field") Operator operator, @Description("The operand of the comparison defined in operator field") String value) {

}
