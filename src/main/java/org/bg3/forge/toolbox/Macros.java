package org.bg3.forge.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Macros {
    public static class Macro {
        public String function;
        public String[] args = new String[0];

        public static Macro fromString(String macroString) {

            int index = macroString.indexOf("(");
            if (index == -1) {
                return null;
            }
            Macro macro = new Macro();
            String function = macroString.substring(0, index).trim();
            macro.function = function;
            String params = macroString.substring(index + 1, macroString.lastIndexOf(")"));
            if (params.isEmpty()) {
                return macro;
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
            macro.args = argList.toArray(new String[argList.size()]);
            return macro;
        }

        public String toString() {
            return function + " " + String.join(" ", args);
        }
    }

    interface DescriptionTransformer {
        String transform(Macro macro);
    }
    static Map<String, DescriptionTransformer> transformers = new HashMap<>();

    static {
        transformers.put("AC", (macro) -> {
            return "Armor Class +" + macro.args[0];
        });
        transformers.put("Ability", (macro) -> {
            String description = macro.args[0];
            description += " +" + macro.args[1];
            if (macro.args.length > 2) {
                description += " (up to " + macro.args[2] + ")";
            }
            return description;
        });
        transformers.put("AbilityOverrideMinimum", (macro) -> {
            return "Set the wearer's " + macro.args[0] + " to " + macro.args[1] + " unless the wearer's " + macro.args[0] + " is already higher";
        });
        transformers.put("ActionResource", (macro) -> {
            int num = Integer.parseInt(macro.args[1]);

            if (macro.args[0].endsWith("Point")) {
                return macro.args[1] + " " + macro.args[0].substring(0, macro.args[1].indexOf("Point")) + " Points";
            }
            
            if (macro.args[0].endsWith("SpellSlot")) {
                String description = macro.args[1];
                if (!macro.args[0].equals("SpellSlot")) {
                    description += " " + macro.args[0].substring(0, macro.args[0].indexOf("SpellSlot")) + " Spell Slots of Level " + macro.args[2];
                }
                return description;
            }

            if (macro.args[0].equals("Movement")) {
                int speed = Integer.parseInt(macro.args[1]);
                return "Movement Speed " + (speed > 0 ? "+" : "") + speed + "m";
            }
            if (macro.args[0].equals("SuperiorityDie")) {
                return macro.args[1] + "Superiority Dice ";
            }
            return "Armor Class +" + macro.args[0];
        });
        transformers.put("AddProficiencyToAC", (macro) -> {
            return "Add Proficiency to Armor Class";
        });
        transformers.put("AddProficiencyToDamage", (macro) -> {
            return "Add Proficiency to Damage";
        });
        transformers.put("Advantage", (macro) -> {
            if (macro.args.length == 2) {
                return "Advantage on " + macro.args[1] + " checks";
            }
            if (macro.args[0].equals("AttackRoll")) {   
                return "Advantage on " + macro.args[1] + " attack rolls";
            }
            if (macro.args[0].equals("AllAbilities")) {
                return "Advantage on all ability checks";
            }
            if (macro.args[0].equals("AllSavingThrows")) {
                return "Advantage on all saving throws";
            }
            if (macro.args[0].equals("Concentration")) {
                return "Advantage on Concentration checks";
            }

            return "Advantage on " + macro.args[0] + " checks";
        });
        transformers.put("CannotBeDisarmed", (macro) -> {
            return "Cannot be disarmed";
        });
        transformers.put("CarryCapacityMultiplier", (macro) -> {
            float multiplier = Float.parseFloat(macro.args[0]);
            multiplier = multiplier * 100;
            int percent = (int) multiplier - 100;
            return "Carry Capacity Increased by "  + percent + "%";
        });
        transformers.put("CharacterUnarmedDamage", (macro) -> {
            String description = "Your unarmed attacks deal an additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            return description;
        });
        transformers.put("CharacterWeaponDamage", (macro) -> {
            String description = "Your weapon attacks deal an additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            return description;
        });
        transformers.put("CriticalDamageOnHit", (macro) -> {
            return "Deal critical damage on hit";
        });
        transformers.put("CriticalHitExtraDice", (macro) -> {
            String description = "Your critical hits deal an additional " + macro.args[0] + " damage roll";
            if (macro.args[1].equals("MeleeOffHandWeaponAttack")) {
                description += " with your off-hand weapon";
            } else if (macro.args[1].equals("RangedWeaponAttack")) {
                description += " with your ranged weapon";
            } else if (macro.args[1].equals("MeleeUnarmedAttack")) {
                description += " with your unarmed attack";
            } else if (macro.args[1].equals("MeleeWeaponAttack")) {
                description += " with your melee weapon";
            }
            return description;
        });
        transformers.put("DamageBonus", (macro) -> {
            String description = "Additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            return description;
        });
         


 
  
    }

    


}
