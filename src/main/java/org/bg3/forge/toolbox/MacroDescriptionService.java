package org.bg3.forge.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bg3.forge.scanner.StatsCollector.Stat;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MacroDescriptionService {
    @Inject
    LibraryService bg3DB;

    @Inject
    DescriptionService descriptionService;

    @PostConstruct
    public void init() {
        initTransformers();
    }

    public static String[] splitMacro(String macroString) {
        int depth = 0;
        List<String> argList = new ArrayList<>();
        int paramIndex = 0;
        for (int index = 0; index < macroString.length(); index++) {
            if (macroString.charAt(index) == '(') {
                depth++;
            } else if (macroString.charAt(index) == ')') {
                depth--;
            } else if (depth == 0) {
                if (macroString.charAt(index) == ';') {
                    argList.add(macroString.substring(paramIndex, index).trim());
                    paramIndex = index + 1;
                } else if (index + 1 == macroString.length()) {
                    argList.add(macroString.substring(paramIndex).trim());
                }
            }
        }
        return argList.toArray(new String[argList.size()]);
    }

    public String description(String macroString) {
        Macro macro = fromString(macroString);
        if (macro != null) return macro.transformer.transform(macro);
        else return null;
    }

    public Macro fromString(String macroString) {

        int index = macroString.indexOf("(");
        if (index == -1) {
            return null;
        }
        Macro macro = new Macro();
        String function = macroString.substring(0, index).trim();
        macro.function = function;
        macro.transformer = transformers.get(function);
        if (macro.transformer == null) {
            Log.debug("No transformer for " + macroString);
            macro.transformer = (m) -> {
                return macroString;
            };
            return macro;
        }
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
            } else if (depth == 0) {
                if (params.charAt(index) == ',') {
                    argList.add(params.substring(paramIndex, index).trim());
                    paramIndex = index + 1;
                } else if (index + 1 == params.length()) {
                    argList.add(params.substring(paramIndex).trim());
                }
            }
        }
        macro.args = argList.toArray(new String[argList.size()]);
        return macro;
    }

    public class Macro {
        public String function;
        public String[] args = new String[0];
        public DescriptionTransformer transformer;

    }

    public interface DescriptionTransformer {
        String transform(Macro macro);
    }

    static Map<String, String> dieRollTargets = Map.of(
        "Attack", "Attack",
        "DeathSavingThrow", "Death Saving Throws",
        "MeleeSpellAttack", "Melee Spell Attack",
        "MeleeUnarmedAttack", "Unarmed Attack",
        "MeleeWeaponAttack", "Weapon Attack",
        "RangedOffHandWeaponAttack", "Ranged Offhand Weapon Attack",
        "RangedSpellAttack", "Spell Attack",
        "RangedUnarmedAttack", "Ranged Unarmed Attack",
        "RangedWeaponAttack", "Ranged Weapon Attack"
    );


    Map<String, DescriptionTransformer> transformers = new HashMap<>();


    void initTransformers() {
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
                return "Advantage on attack rolls";
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
         
        transformers.put("DamageReduction", (macro) -> {
            String description = "Reduce " + macro.args[0] + " damage by ";

            if (macro.args[1].equals("Flat")) {
                description += macro.args[2];
            } else if (macro.args[1].equals("Half")) {
                description += "Half";
            }
            return description;
        });
        transformers.put("DarkvisionRange", (macro) -> {
            String description = "Gain darkvision up to " + macro.args[0] + "m";
            return description;
        });
        transformers.put("DarkvisionRangeMin", (macro) -> {
            String description = "Gain darkvision up to " + macro.args[0] + "m";
            return description;
        });
        transformers.put("Disadvantage", (macro) -> {
            if (macro.args.length == 2) {
                return "Disadvantage on " + macro.args[1] + " checks";
            }
            if (macro.args[0].equals("AttackRoll")) {   
                return "Disadvantage on attack rolls";
            }
            if (macro.args[0].equals("AllAbilities")) {
                return "Disadvantage on all ability checks";
            }
            if (macro.args[0].equals("AllSavingThrows")) {
                return "Disadvantage on all saving throws";
            }
            if (macro.args[0].equals("Concentration")) {
                return "Disadvantage on Concentration checks";
            }

            return "Disadvantage on " + macro.args[0] + " checks";
        });
        transformers.put("Initiative", (macro) -> {
            return "Add " + macro.args[0] + " to initiative";
        });
        transformers.put("IgnoreResistance", (macro) -> {
            if (macro.args[1].equals("Resistant")) {
                return "Ignore resistance to " + macro.args[0];
            } else {
                return "Ignore immunity to " + macro.args[0];
            }
        });
      
        transformers.put("ItemReturnToOwner", (macro) -> {
            return "Weapon returns to you when thrown or dropped.";
        });
        transformers.put("JumpMaxDistanceBonus", (macro) -> {
            String description = "Increase jump distance by " + macro.args[0] + "m";
            return description;
        });
        transformers.put("JumpMaxDistanceBonus", (macro) -> {
            String description = "Increase jump distance by " + macro.args[0] + "m";
            return description;
        });
        transformers.put("ItemReturnToOwner", (macro) -> {
            return "Weapon returns to you when thrown or dropped.";
        });
        transformers.put("MaximizeHealing", (macro) -> {
            return "Weapon returns to you when thrown or dropped.";
        });
        transformers.put("MinimumRollResult", (macro) -> {
            return "Cannot roll less than " + macro.args[1] + " for " + macro.args[0];
        });
        transformers.put("Proficiency", (macro) -> {
            return "Gain proficiency in " + macro.args[0];
        });
        transformers.put("ProficiencyBonus", (macro) -> {
            return "Add your proficiency bonus to " + macro.args[1] + " " + macro.args[0];
        });

        transformers.put("ReduceCriticalAttackThreshold", (macro) -> {
            return "Reduce the number to roll a critical strike by " + macro.args[0];
        });
        transformers.put("Reroll", (macro) -> {
            return "Re-roll " + macro.args[0] + " if die is " + macro.args[1] + " or less." + (macro.args[2].equals("true") ? " Keep the higher result." : " You must keep that new roll.");
        });
        transformers.put("Resistance", (macro) -> {
            return "You are " + macro.args[1] + " to " + macro.args[0];
        });
        transformers.put("RollBonus", (macro) -> {
            String description = "";
            if (macro.args.length == 3) {
                description += macro.args[2] + " ";
            }
            if (dieRollTargets.containsKey(macro.args[0])) {
                description += dieRollTargets.get(macro.args[0]) + " ";
            } else if (macro.args[0].equals("SkillCheck")) {
                if (macro.args.length == 2) {
                    description += "Skill Checks ";
                } else {
                    description += "Checks ";
                }
            } else if (macro.args[0].equals("RawAbility")) {
                if (macro.args.length == 2) {
                    description += "All Ability Checks ";
                } else {
                    description += "Checks ";
                }
            } else if (macro.args[0].equals("SavingThrow")) {
                if (macro.args.length == 2) {
                    description +="All ";
                }
                description += "Saving Throws ";
            } else {
                description += macro.args[0];
            }
            description += "+" + macro.args[1];
            return description;
        });
        transformers.put("Skill", (macro) -> {
            return macro.args[0] + " +" + macro.args[1];
        });
         transformers.put("SpellSaveDC", (macro) -> {
            return "Spell Save DC +" + macro.args[0];
        });
        transformers.put("UnlockSpell", (macro) -> {
            Stat stat = bg3DB.library().statsCollector.getByName(macro.args[0]);
            if (stat == null) {
                return "UnlockSpell (no stat)" + macro.args[0];
            }
            String displayName = descriptionService.statDisplayName(stat);
            if (displayName == null) {
                return "UnlockSpell (no display name)" + macro.args[0];
            }
            return "Spell: " + displayName;
        });
        transformers.put("WeaponDamage", (macro) -> {
            String description = "Additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            return description;
        });
        transformers.put("WeaponEnchantment", (macro) -> {
            return "Weapon Enchantment +" + macro.args[0];
        });
   
        transformers.put("Weapon Property", (macro) -> {
            if (macro.args[0].equals("Magical")) {
                return "Weapon is magical.";
            } else {
                return null;
            }
        });
   


 
  
    }

    


}
