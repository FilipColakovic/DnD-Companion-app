# PHB 2024 character-choice reminders

The Character page shows pending creation choices with checkboxes and a **Select** action. The action requires the exact number of distinct options. Confirmed reminders disappear; **Review saved choices** exposes their summaries and **Edit** controls. Incomplete checkbox drafts survive unrelated resource refreshes. Restoring a checkpoint discards those drafts.

Coverage includes the sixteen PHB 2024 backgrounds, ten species and their existing selectable lineages:

| Source | Required creation choices |
| --- | --- |
| Sorcerer | Two skills: Arcana, Deception, Insight, Intimidation, Persuasion, Religion |
| Aasimar | Small or Medium size |
| Dragonborn | One of the ten draconic ancestors |
| Dwarf | No creation choice beyond species selection |
| Elf | Insight, Perception, or Survival; INT, WIS, or CHA for lineage spells |
| Gnome | INT, WIS, or CHA for lineage spells |
| Goliath | One of six Giant Ancestry benefits |
| Halfling | No further creation choice |
| Human | Small or Medium; any one skill; any PHB Origin feat |
| Orc | No further creation choice |
| Tiefling | Small or Medium; INT, WIS, or CHA for legacy spells |

Elf/Gnome/Tiefling lineage or legacy is already selected in the species control. Fixed lineage spells remain automatic grants. High Elf's initial cantrip is Prestidigitation; replacing it after a Long Rest is optional, not an unresolved creation choice. Aasimar revelation and Dragonborn breath shape are chosen when used, rather than locked at creation.

Selected Skilled, Crafter, Musician, Magic Initiate, Skill Expert, Keen Mind and Observant feats have follow-up forms on Character. Skill Expert expertise options require a proficient skill without other expertise; Keen Mind and Observant record proficiency or expertise according to training when confirmed. Magic Initiate offers all PHB 2024 cantrips and level-one spell names from the chosen Cleric, Druid, or Wizard list, including entries omitted from the Free Rules. Spell names are configuration data, not a replacement for the bundled spell-details database. Feat spell choices are recorded and displayed; automatic addition of those spells to the casting engine and other feat effect automation remain separate work. Tool choices are listed under Training & Gear. Ancestry and size choices are recorded under Features & Traits.

Class, species, background and configured feat skill grants apply immediately to displayed bonuses, passive senses, skill rolls, and printed sheets. Proficiency does not stack; manual expertise is retained. Changing species deactivates its skill and Origin-feat grants while retaining those choices for switching back. Manual proficiencies and manually equipped feats remain intact. Clicking a granted skill marker toggles expertise and its underlying proficiency; removing that proficiency requires editing its source choice.

Lineage spellcasting ability affects spell save DCs and attack rolls. A spell also learned as a Sorcerer spell uses its class Charisma ability in the current casting interface. Source selection for overlapping spell grants is not added here.

Choices use validated `sheet.choice.*` properties inside the existing play-state encoding and SQLite transaction. There is no database migration. Old saves show reminders instead of inferring which source supplied an existing manual proficiency. The save lock and checkpoint restoration continue to apply.

Rule references reviewed for this implementation:

- [PHB 2024 backgrounds and Origin feats](https://www.dndbeyond.com/posts/1785-the-backgrounds-and-origin-feats-in-the-2024), with each catalog card linking to its corresponding [2024 background entry](https://roll20.net/compendium/dnd5e/Backgrounds%3ANoble?expansion=32231)
- [Skill Expert](https://roll20.net/compendium/dnd5e/Feats%3ASkill%20Expert?expansion=32231), [Keen Mind](https://roll20.net/compendium/dnd5e/Feats%3AKeen%20Mind?expansion=32231), [Observant](https://roll20.net/compendium/dnd5e/Feats%3AObservant?expansion=32231)

- [2024 Sorcerer and species rules](https://www.dndbeyond.com/sources/dnd/br-2024/character-classes), [Character Origins](https://www.dndbeyond.com/sources/dnd/br-2024/character-origins)
- [The ten PHB 2024 species](https://www.dndbeyond.com/posts/1783-the-10-species-in-the-2024-players-handbook)
- [PHB Wizard spell list](https://app.roll20.net/compendium/dnd5e/Classes:Wizard?expansion=32231), [Cleric spell list](https://app.roll20.net/compendium/dnd5e/Classes:Cleric?expansion=32231), [Druid spell list](https://app.roll20.net/compendium/dnd5e/Classes:Druid?expansion=32231)
- [Magic Initiate and Skilled](https://www.dndbeyond.com/sources/dnd/br-2024/feats), [PHB crafting and Crafter](https://www.dndbeyond.com/posts/1788-lets-explore-the-crafting-rules-in-the-2024), [tools and instruments](https://www.dndbeyond.com/sources/dnd/br-2024/equipment)

Verification: `org.example.ui.character.BackgroundChecks` and `org.example.ui.character.CharacterChoicesChecks`, plus the existing character, Feats, species, combat, dice, spell-library and SQLite checks. Checks use disposable state and never edit the real character database.

Backgrounds are selected in **Feats → Backgrounds**. Their two fixed skills and their Origin feat are derived grants; changing or removing the background deactivates only that source. Free-form legacy background notes are preserved until the user explicitly selects a catalog background. Structured selections make the Character background name read-only, with a **Choose background** link back to the catalog.

Background ability allocations permit +2/+1 across two of the background's three abilities, or +1 to each. They are recorded rather than added to already entered final scores. Starting equipment records the PHB package versus 50 GP; inventory and currency remain manual. Variable tools and feat choices have compact Character forms. Background-origin and Human-origin repeatable feat configurations use separate keys, so two Skilled instances grant independently chosen proficiencies. Acolyte, Guide and Sage fix their respective Magic Initiate spell lists; a second instance must use a different list.

The small red **!** next to Character uses the same pending-decision count as the reminders. It updates on choice confirmation, background/species changes, feat selection/removal and checkpoint restoration. Clicking it opens Character. It signals unfinished active decisions, not unsaved edits.
Automatic maximum HP uses the PHB fixed Sorcerer values and current Constitution, plus Dwarf, Draconic Sorcery, Tough and Boon of Fortitude bonuses. These HP bonuses are now applied automatically, unlike other unimplemented feat effects. Current HP tracks outstanding damage and remains zero until explicitly healed. Manual maximum HP remains available for rolled HP and legacy characters. Automatic HP uses the existing zero-maximum sentinel with validated damage/zero/mode properties; no SQLite migration is needed.

HP rules: [level-one and fixed advancement HP](https://www.dndbeyond.com/sources/dnd/br-2024/creating-a-character), [Draconic Resilience](https://roll20.net/compendium/dnd5e/Other%20Options%20and%20Features%3ADraconic%20Resilience?expansion=33335), [Tough](https://roll20.net/compendium/dnd5e/Feats%3ATough?expansion=32231), [Boon of Fortitude](https://roll20.net/compendium/dnd5e/Feats%3ABoon%20of%20Fortitude?expansion=32231). Verification includes `org.example.ui.character.HitPointChecks`.