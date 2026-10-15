# PHB 2024 feat effects

Selecting a feat applies its fixed benefits immediately. Feats with ability or spell choices add compact Character reminders and light the red **!** beside Character. Confirm the choice to apply its benefit. Review saved choices to change it later. All 75 catalog feats retain their PHB category, level availability, prerequisite confirmation and source link.

The large ability number is the **total score (modifier)**; the editable spinner is the **base score**. General feat increases cap at 20 and Epic increases at 30. Bonuses are derived from equipped feats, so switching a choice, removing a feat or restoring a checkpoint reverses its effects. Scores, saves, skills, passive senses, spell attacks/DCs and automatic HP use these totals. Manual HP overrides remain manual. Existing background ability allocations still record already-entered final scores; they are not added again.

Repeatable ASI, Elemental Adept, Skilled and Magic Initiate acquisitions can be configured through **Copies acquired** on Character. Record copies actually acquired; this control does not award additional advancement milestones. Each additional copy has its own choices. Elemental Adept types and Magic Initiate class lists must differ. Background copies use their existing separate source identifiers.

## Automatic spells and casting sources

- Telekinetic grants Mage Hand immediately, with no V/S components, an invisible hand and 60-foot range/distance. Its chosen mental ability supplies the shove DC.
- Telepathic prepares Detect Thoughts. Its daily free cast removes components; ordinary slot casts use the spell's normal components.
- Fey-Touched prepares Misty Step and a chosen PHB level-one Divination/Enchantment spell. Shadow-Touched prepares Invisibility and a chosen level-one Illusion/Necromancy spell.
- Magic Initiate grants the chosen two cantrips and level-one spell from its confirmed class list, using its chosen spellcasting ability. A background's fixed list is respected.
- Ritual Caster prepares the chosen PB level-one rituals. Increasing PB prompts for another ritual while keeping the existing grants. Ordinary rituals and the once-per-Long-Rest Quick Ritual share no spell-slot cost; Quick Ritual has one shared use across eligible prepared rituals.

Automatic feat spells do not occupy Sorcerer preparation/cantrip limits. Duplicate spell names appear once. Removing a source preserves overlapping class/species/background grants and manual learning. The bundled spell database now contains 219 records, including every spell offered by feat choices; the normal Sorcerer library is still filtered to the character's applicable catalog.

The **Casting source** control chooses spell slots, species free casts or a particular feat's free cast. Each feat/source/spell has its own daily counter; Magic Initiate background and acquired copies are independent. Free casts use base spell level and the selected source's ability. Rest resets, saved counters and failed-cast atomicity use the existing checkpoint/save mechanism. Spell Recall rolls a d4 on level-one through level-four slot casts and records whether the slot was preserved.

## Equipment and combat effects

Character's Training & Gear contains worn armor, shield, main/off-hand weapon and Healer's Kit uses. Armor and shield determine AC. Lightly/Moderately/Heavily Armored provide their training, Martial Weapon Training provides martial proficiency, and Medium Armor Master changes the medium-armor DEX cap. Untrained armor/shields block casting before spending resources; legacy AC overrides remain available.

Expand **Equipment & feat rolls** in Cast & Combat for weapon damage, saving throws, initiative and incoming damage. Weapon calculations include Archery, Dueling, Thrown/Two-Weapon/Great Weapon Fighting, Crossbow Expert's extra-attack modifier, Great Weapon Master, Piercer critical dice, Tavern Brawler and Unarmed Fighting. Weapon Master annotates the chosen proficient weapon's mastery. Heavy Armor Master reduces eligible physical attack damage; Energy Resistance and Night Spirit use the selected types/lighting. Damage consumes temporary HP before current HP. Shield and two-hand circumstances must be consistent.

Character and combat provide **Feat actions** for triggered benefits. Their tooltips and confirmation describe the actual trigger; limited pools are tracked and saved. Lucky's two actions share PB points. Mage Slayer and Fate reset on Short/Long Rest; Fate also resets on initiative. Recovery's Last Stand and ten-d10 pool are separate. Chef tracks prepared treats; Poisoner tracks brewed doses and their DC/damage; Healer consumes kit uses and Hit Dice. Fast Crafting checks proficient tools, records one selected temporary item and expires it on the next Long Rest. Inspiring Leader's temporary HP does not stack. Musician records the eligible ally grant. Currency, ingredients and allies' own sheets remain player-managed.

War Caster and Durable apply advantage to the corresponding save controls. Actor/Skulker add conditional skill check controls. Grappler/Mounted Combatant advantage requires the qualifying target checkbox. Elemental Adept adjusts supported spell damage dice and annotates ignored resistance; Poisoner annotates Poison resistance bypass. Cure Wounds/Healing Word rolls include Healer's one-time reroll of each initial 1. Telekinetic/Spell Sniper modify live range/components displays without altering the published spell record. Blindsight, Truesight, training and resistance summaries appear under Training & Gear.

## Limits of automatic resolution

The app owns the active character, not an enemy roster, ally sheets, world geometry or a turn clock. Pushes, grapples, prone, mount defense, concentration breaking, enemy disadvantage, opportunity attacks, critical-trigger effects and spell reaction targets are calculated or described in the combat log after confirming their trigger. Apply those outcomes to the relevant creature at the table. Once-per-turn triggers are player-confirmed; there is no restored Next/End Turn mechanic. Hit/critical and Attack-action checkboxes express known outcomes; rolling damage does not cast a spell or establish that an attack hit. Preparation choices do not waive published prerequisites.

Equipment selectors cover mundane PHB armor/weapons. They do not automatically buy equipment, subtract gold, manage ammunition, apply magic-item bonuses or determine enemies' defenses. Resistance immunity is resolved separately. Persisted base inputs and existing save formats are retained; checks never modify real character saves. Legacy storage is imported once into the UUID roster database.

## Rules and verification

Sources: [PHB 2024 feat entries](https://roll20.net/compendium/dnd5e/Rules%3AFeat%20Descriptions?expansion=32231), [Free Rules feats](https://www.dndbeyond.com/sources/dnd/br-2024/feats), [Telekinetic](https://roll20.net/compendium/dnd5e/Feats%3ATelekinetic?expansion=32231), [Ritual Caster](https://roll20.net/compendium/dnd5e/Feats%3ARitual%20Caster?expansion=32231), [equipment](https://www.dndbeyond.com/sources/dnd/br-2024/equipment). Spell attribution remains in [SPELL_DATA_LICENSE.md](SPELL_DATA_LICENSE.md).

`scripts/Verify.ps1` includes `FeatEffectsChecks` (all feat ASIs/choices, complete spell options, source removal, overlapping grants, save encoding, equipment and atomic resource failures) and `FeatAutomationUiChecks` (real Character confirmations, sidebar marker, casting-source buttons and 16 rendered theme/size/width previews). Other existing character, database, combat, spell, Metamagic, appearance and checkpoint checks remain in the same runner. All fixtures are disposable.

New proficiency-icon expertise upgrades record their source dependencies. Removing Skilled, an individual repeatable copy, a background or a species deactivates its grants and dependent upgrades, preserving independent manual training. Dormant configuration remains saved and is revalidated before reactivation. New characters also derive background ability increases over their base scores; legacy scores remain untouched. `FeatRemovalChecks` and real Character-control checks cover these cases.

Use **Add acquired copy** in a repeatable feat's inspector to record an additional advancement grant. **Copies acquired** on Character controls how many numbered copies are active; decreasing it leaves higher-copy configuration dormant. New Human Skilled/Magic Initiate selections use `human.feat.*` configuration independent of background and acquired copies. Existing legacy keys remain supported without guessing their origin.
