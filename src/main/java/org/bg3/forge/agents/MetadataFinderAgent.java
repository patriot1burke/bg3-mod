package org.bg3.forge.agents;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import org.bg3.forge.model.MetadataFilter;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@ApplicationScoped
@RegisterAiService
public interface MetadataFinderAgent {

    @SystemMessage("""
Your task is to extract metadata from a natural language query about Baldur's Gate 3 weapons and armor, and convert it into an array of JSON filter objects and no other text.

---

JSON Structure

Return an array of JSON objects with the following fields:
- `type` (string, optional)
- `slot` (string, optional)
- `name` (string, optional)

Metadata Field Semantics

- The name slot should only be filled out if the user asks for a name. i.e. Find The Holy Avenger Sword.  If a name is specified, ignore all other fields
- The "type" field is either "Weapon" or "Armor".
- If the user is asking about any kind of weapon, add the "type" field with value "Weapon"
- If the user is querying about any kind of armor, ring, amumlet, necklace, or cloak, add the "type" field with the value "Armor"
- The "slot" field can be "Head", "Breast", "Gloves", "Boots", "Ring", "Amulet", "Cloak", "MainHand", "Ranged"
- If the user is asking about a weapon that shoots, then the "slot" field should be set to "Ranged", otherwise, it should be set to "MainHand"

---

Examples

Input:
Show me magical longbows with damage > 6 and not cursed

Output:
[
    {
       "type": "Weapon",
       "slot": "Ranged"

    }
]
---

Input:
Show me all daggers and bows

Output:
[
    {
       "type": "Weapon",
       "slot": "MainHand"

    },   
    {
       "type": "Weapon",
       "slot": "Ranged"

    }
]

---

Examples

Input:
Find me all magic head and glove pieces.

Output:
[
    {
       "type": "Armor",
       "slot": "Gloves"

    }
    {
       "type": "Armor",
       "slot": "Head"
    }
]

---

        """)
    String answer(@UserMessage String query);

}
