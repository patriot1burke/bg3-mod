package org.bg3.forge.model;

import dev.langchain4j.model.output.structured.Description;

public record Filter(@Description("A comparator for the value field") Operator operator, @Description("The value of the comparison") String value) {

}
