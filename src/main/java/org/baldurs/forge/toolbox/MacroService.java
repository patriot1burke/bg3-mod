package org.baldurs.forge.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.baldurs.forge.scanner.StatsArchive.Stat;
import org.baldurs.forge.toolbox.BoostService.BoostWriter;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MacroService {
    @Inject
    LibraryService bg3DB;

    @Inject
    BoostService descriptionService;

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
            }
            if (depth == 0) {
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

    public boolean isMacro(String macroString) {    
        return macroString.indexOf('(') > -1;
    }

    public void writeMacro(String macroString, BoostWriter writer) {
        Macro macro = fromString(macroString);
        if (macro != null) macro.transformer.transform(macro, writer);
    }

    public Macro fromString(String macroString) {

        int index = macroString.indexOf('(');
        if (index == -1) {
            return null;
        }
        Macro macro = new Macro();
        String function = macroString.substring(0, index).trim();
        macro.function = function;
        macro.transformer = transformers.get(function);
        if (macro.transformer == null) {
            Log.debug("No transformer for " + macroString);
            macro.transformer = (m, writer) -> {
                writer.writeUnknown(macroString);
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
            } 
            if (depth == 0) {
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
        void transform(Macro macro, BoostWriter writer);
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
        transformers.put("AC", (macro, writer) -> {
            writer.write("Armor Class +" + macro.args[0]);
        });
        transformers.put("Ability", (macro, writer) -> {
            String description = macro.args[0];
            description += " +" + macro.args[1];
            if (macro.args.length > 2) {
                description += " (up to " + macro.args[2] + ")";
            }
            writer.write(description);
        });
        transformers.put("AbilityOverrideMinimum", (macro, writer) -> {
            writer.write("Set the wearer's " + macro.args[0] + " to " + macro.args[1] + " unless the wearer's " + macro.args[0] + " is already higher");
        });
        transformers.put("ActionResource", (macro, writer) -> {
            if (macro.args[0].endsWith("Point")) {
                writer.write(macro.args[1] + " " + macro.args[0].substring(0, macro.args[1].indexOf("Point")) + " Points");
                return;
            }
            
            if (macro.args[0].endsWith("SpellSlot")) {
                String description = macro.args[1];
                if (!macro.args[0].equals("SpellSlot")) {
                    description += " " + macro.args[0].substring(0, macro.args[0].indexOf("SpellSlot")) + " Spell Slots of Level " + macro.args[2];
                }
                writer.write(description);
                return;
            }

            if (macro.args[0].equals("Movement")) {
                int speed = Integer.parseInt(macro.args[1]);
                writer.write("Movement Speed " + (speed > 0 ? "+" : "") + speed + "m");
                return;
            }
            if (macro.args[0].equals("SuperiorityDie")) {
                writer.write(macro.args[1] + "Superiority Dice ");
                return;
            }
            writer.write("Action Resource" + macro.args[0]);
        });
        transformers.put("AddProficiencyToAC", (macro, writer) -> {
            writer.write("Add Proficiency to Armor Class");
        });
        transformers.put("AddProficiencyToDamage", (macro, writer) -> {
            writer.write("Add Proficiency to Damage");
        });
        transformers.put("Advantage", (macro, writer) -> {
            if (macro.args.length == 2) {
                writer.write("Advantage on " + macro.args[1] + " checks");
            }
            if (macro.args[0].equals("AttackRoll")) {   
                writer.write("Advantage on attack rolls");
            }
            if (macro.args[0].equals("AllAbilities")) {
                writer.write("Advantage on all ability checks");
            }
            if (macro.args[0].equals("AllSavingThrows")) {
                writer.write("Advantage on all saving throws");
            }
            if (macro.args[0].equals("Concentration")) {
                writer.write("Advantage on Concentration checks");
            }

            writer.write("Advantage on " + macro.args[0] + " checks");
        });
        transformers.put("CannotBeDisarmed", (macro, writer) -> {
            writer.write("Cannot be disarmed");
        });
        transformers.put("CarryCapacityMultiplier", (macro, writer) -> {
            float multiplier = Float.parseFloat(macro.args[0]);
            multiplier = multiplier * 100;
            int percent = (int) multiplier - 100;
            writer.write("Carry Capacity Increased by "  + percent + "%");
        });
        transformers.put("CharacterUnarmedDamage", (macro, writer) -> {
            String description = "Your unarmed attacks deal an additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            writer.write(description);
        });
        transformers.put("CharacterWeaponDamage", (macro, writer) -> {
            String description = "Your weapon attacks deal an additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            writer.write(description);
        });
        transformers.put("CriticalDamageOnHit", (macro, writer) -> {
            writer.write("Deal critical damage on hit");
        });
        transformers.put("CriticalHitExtraDice", (macro, writer) -> {
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
            writer.write(description);
        });
        transformers.put("DamageBonus", (macro, writer) -> {
            String description = "Additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            writer.write(description);
        });
         
        transformers.put("DamageReduction", (macro, writer) -> {
            String description = "Reduce " + macro.args[0] + " damage by ";

            if (macro.args[1].equals("Flat")) {
                description += macro.args[2];
            } else if (macro.args[1].equals("Half")) {
                description += "Half";
            }
            writer.write(description);
        });
        transformers.put("DarkvisionRange", (macro, writer) -> {
            String description = "Gain darkvision up to " + macro.args[0] + "m";
            writer.write(description);
        });
        transformers.put("DarkvisionRangeMin", (macro, writer) -> {
            String description = "Gain darkvision up to " + macro.args[0] + "m";
            writer.write(description);
        });
        transformers.put("Disadvantage", (macro, writer) -> {
            if (macro.args.length == 2) {
                writer.write("Disadvantage on " + macro.args[1] + " checks");
            }
            if (macro.args[0].equals("AttackRoll")) {   
                writer.write("Disadvantage on attack rolls");
            }
            if (macro.args[0].equals("AllAbilities")) {
                writer.write("Disadvantage on all ability checks");
            }
            if (macro.args[0].equals("AllSavingThrows")) {
                writer.write("Disadvantage on all saving throws");
            }
            if (macro.args[0].equals("Concentration")) {
                writer.write("Disadvantage on Concentration checks");
            }

            writer.write("Disadvantage on " + macro.args[0] + " checks");
        });
        transformers.put("Initiative", (macro, writer) -> {
            writer.write("Add " + macro.args[0] + " to initiative");
        });
        transformers.put("IgnoreResistance", (macro, writer) -> {
            if (macro.args[1].equals("Resistant")) {
                writer.write("Ignore resistance to " + macro.args[0]);
            } else {
                writer.write("Ignore immunity to " + macro.args[0]);
            }
        });
      
        transformers.put("ItemReturnToOwner", (macro, writer) -> {
            writer.write("Weapon returns to you when thrown or dropped.");
        });
        transformers.put("JumpMaxDistanceBonus", (macro, writer) -> {
            String description = "Increase jump distance by " + macro.args[0] + "m";
            writer.write(description);
        });
        transformers.put("MaximizeHealing", (macro, writer) -> {
            writer.write("Weapon returns to you when thrown or dropped.");
        });
        transformers.put("MinimumRollResult", (macro, writer) -> {
            writer.write("Cannot roll less than " + macro.args[1] + " for " + macro.args[0]);
        });
        transformers.put("Proficiency", (macro, writer) -> {
            writer.write("Gain proficiency in " + macro.args[0]);
        });
        transformers.put("ProficiencyBonus", (macro, writer) -> {
            writer.write("Add your proficiency bonus to " + macro.args[1] + " " + macro.args[0]);
        });

        transformers.put("ReduceCriticalAttackThreshold", (macro, writer) -> {
            writer.write("Reduce the number to roll a critical strike by " + macro.args[0]);
        });
        transformers.put("Reroll", (macro, writer) -> {
            writer.write("Re-roll " + macro.args[0] + " if die is " + macro.args[1] + " or less." + (macro.args[2].equals("true") ? " Keep the higher result" : " You must keep that new roll"));
        });
        transformers.put("Resistance", (macro, writer) -> {
            writer.write("You are " + macro.args[1] + " to " + macro.args[0]);
        });
        transformers.put("RollBonus", (macro, writer) -> {
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
            writer.write(description);
        });
        transformers.put("Skill", (macro, writer) -> {
            writer.write(macro.args[0] + " +" + macro.args[1]);
        });
         transformers.put("SpellSaveDC", (macro, writer) -> {
            writer.write("Spell Save DC +" + macro.args[0]);
        });
        transformers.put("UnlockSpell", (macro, writer) -> {
            Stat stat = bg3DB.library().statsCollector.getByName(macro.args[0]);
            if (stat == null) {
                writer.write("<i>UnlockSpell (no stat)" + macro.args[0] + "</i>");
                return;
            }
            String displayName = descriptionService.statDisplayName(stat);
            if (displayName == null) {
                writer.write("<i>UnlockSpell (no display name)" + macro.args[0] + "</i>");
                return;
            }
            writer.unlockSpell(stat);
        });
        transformers.put("WeaponDamage", (macro, writer) -> {
            String description = "Additional " + macro.args[0];
            if (macro.args.length == 2) {
                description += " " + macro.args[1];
            }
            description += " damage";
            writer.write(description);
        });
        transformers.put("WeaponEnchantment", (macro, writer) -> {
            writer.write("Weapon Enchantment +" + macro.args[0]);
        });
   
        transformers.put("WeaponProperty", (macro, writer) -> {
            if (macro.args[0].equals("Magical")) {
                writer.write("Weapon is magical.");
            } else {
                //writer.write(null);
            }
        });
   


 
  
    }

    


}
