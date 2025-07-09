package org.baldurs.forge.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DescriptionParam {
    public interface DescriptionTransformer {
        String transform(DescriptionParam macro);
    }

    public String value;
    public String function;
    public String[] args = new String[0];
    public DescriptionTransformer transformer = macro -> macro.value;

    public String transform() {
        return transformer.transform(this);
    }

    public static String param(String paramString) {
        return fromString(paramString).transform();
    }


    public static DescriptionParam fromString(String paramString) {

        int index = paramString.indexOf("(");
        if (index == -1) {
            DescriptionParam description = new DescriptionParam();
            description.value = paramString;
            return description;
        }
        DescriptionParam descriptionParam = new DescriptionParam();
        String function = paramString.substring(0, index).trim();
        descriptionParam.function = function;
        String params = paramString.substring(index + 1, paramString.lastIndexOf(")"));
        if (params.isEmpty()) {
            return descriptionParam;
        }
        int depth = 0;
        List<String> argList = new ArrayList<>();
        int paramIndex = 0;
        for (index = 0; index < params.length(); index++) {
            if (params.charAt(index) == '(') {
                depth++;
            } else if (params.charAt(index) == ')') {
                depth--;
            } else if (depth == 0 && (params.charAt(index) == ',' || index + 1 == params.length())) {
                argList.add(params.substring(paramIndex, index).trim());
            }
        }
        descriptionParam.args = argList.toArray(new String[argList.size()]);
        DescriptionTransformer descriptionTransformer = transformers.get(function);
        if (descriptionTransformer != null) {
            descriptionParam.transformer = descriptionTransformer;
        }
        return descriptionParam;
    }


    static Map<String, DescriptionTransformer> transformers = new HashMap<>();

    static {
        transformers.put("DealDamage", (param) -> {
            return param.args[0] + " " + param.args[1];
        });
        transformers.put("RegainHitPoints", (param) -> {
            return param.args[0] + " hit points";
        });
        transformers.put("Distance", (param) -> {
            return param.args[0] + "m";
        });
        transformers.put("GainTemporaryHitPoints", (param) -> {
            return param.args[0] + " temporary hit points";
        });
    }
}
