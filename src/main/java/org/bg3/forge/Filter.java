package org.bg3.forge;

import java.util.List;


public record Filter(String field, String operator, Object value, List<Filter> filters) {

    public String toString() {  
        return "Filter(field=" + field + ", operator=" + operator + ", value=" + value + ", filters=[]" + filters + "])";
    }

}
