package org.bg3.forge.db;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Equipment extends PanacheEntity {
    public enum WeaponWeight {
        Light,
        Medium,
        Heavy
    }
    public enum ArmorType {
        Clothing,
        LightArmor,
        MediumArmor,
        HeavyArmor
    }

    public enum DamageType {
        Slashing,
        Piercing,
        Bludgeoning,
        Fire,
        Cold,
        Lightning,
        Necrotic,
        Poison,
        Radiant,
        Thunder,
        Acid,
        Force,
        Psychic
    }

    public enum Rarity {
        Common,
        Uncommon,
        Rare,
        VeryRare,
        Legendary,
    }



    public String name;
    public String type;
    public String slot;
    public String rootTemplate;
    public String description;
    public Rarity rarity;

    // Armor Columns
    public ArmorType armorType;
    public int armorClass;
    public boolean shield;
   

    // Weapon columns
    public String damage;
    public DamageType damageType;


    // Weapon properties
    public boolean twoHanded;
    public boolean thrown;
    public boolean dippable;
    public boolean versatile;
    public boolean finesse;
    public boolean reach;
    public boolean magical;
    public WeaponWeight weaponWeight;

 
    
    
}
