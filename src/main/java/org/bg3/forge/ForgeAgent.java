package org.bg3.forge;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterAiService
public interface ForgeAgent {

    @SystemMessage("""
        You are a helpful assistant that can answer questions about Baldur's Gate 3 items.
        From the following data (in JSON format):
        {json}
        Answer user questions in natural langauge using the data provided above.  When listing items, only specify the name of it.
        """)
    String answer(@UserMessage String question, String json);

    @SystemMessage("""
Your task is to extract metadata from a natural language query about Baldur's Gate 3 weapons and armor, and convert it into a single JSON filter object that may be recursively composed.

---

JSON Structure

Return a single object with the following format:

{
  "metadata": "<metadata field name>",
  "operator": "=" | "!=" | ">" | "<" | ">=" | "<=" | "and" | "or",
  "value": <literal>,         // Used only for basic comparisons
  "filters": [<filter>, ...]  // Used only for compound boolean expressions
}
- Valid "metadata" values are "type", "slot", "name".
- Use "filters" only if operator is "and" or "or".
- Use "value" only if comparing against a literal value.
- Do not include both value and filters in the same object.

---

Metadata Field Semantics

- "type": "Weapon" or "Armor"
- "slot": "Head", "Breast", "Gloves", "Boots", "Ring", "Amulet", "Cloak", "MainHand", "Ranged"
---

Slot and Type Inference

Infer "type" and "slot" based on known item categories:

- If the user asks for daggers swords, clubs, maces, staves, and short swords these are all of type: "Weapon" and a slot of "MainHand".  
- "Ranged" → bows, longbows, crossbows → type: "Weapon"
- "Breast" → chest armor, body armor → type: "Armor"
- "Head" → helmet, helm
- Gloves, boots, rings, amulets, cloaks → appropriate slot, type: "Armor"

---

Examples

Input:
Show me magical longbows with damage > 6 and not cursed

Output:
{
  "operator": "and",
  "filters": [
    {
      "metadata": "type",
      "operator": "=",
      "value": "Weapon"
    },
    {
      "metadata": "slot",
      "operator": "=",
      "value": "Ranged"
    }
  ]
}

---

Input:
Show me daggers

Output:
{
  "operator": "and",
  "filters": [
    {
      "metadata": "type",
      "operator": "=",
      "value": "Weapon"
    },
    {
      "metadata": "slot",
      "operator": "=",
      "value": "MainHand"
    }
  ]
}

---

Constraints

- Return a single top-level "filter" object.
- Use "filters" only with "and" or "or".
- Use "value" only with comparison operators (=, !=, >, <, >=, <=).
- Output valid JSON. No extra text or explanations.

---

JSON Output:
Return a single, valid JSON "filter" object.
""")
        Filter filter(@UserMessage String question);       

}
