# Spell data

This work includes material from the System Reference Document 5.2.1 ("SRD 5.2.1")
by Wizards of the Coast LLC, available at https://www.dndbeyond.com/srd.
The SRD 5.2.1 is licensed under the Creative Commons Attribution 4.0 International
License, available at https://creativecommons.org/licenses/by/4.0/legalcode.

The structured spell transcription is from
[Open5e's SRD 2024 Spell.json](https://github.com/open5e/open5e-api/blob/main/data/v2/wizards-of-the-coast/srd-2024/Spell.json).
The reviewed data is stored in `scripts/data/spell-catalog.tsv` and bundled in
`src/main/resources/spells/spells.db`, built by `scripts/Generate-SpellDetails.ps1`.
SRD spell descriptions and mechanics are retained. The spellbook displays shortened
first-sentence excerpts for its list summaries. Named PHB spells are mapped to their
SRD equivalents: Arcane Hand, Black Tentacles, Freezing Sphere, and Telepathic Bond.
Summoned spirit stat blocks are added as original summaries of the mechanics.
An empty Greater Invisibility description in the transcription is repaired using
the official SRD 5.2.1, page 137. Casting-time labels are normalized for display.
Additional transcription corrections are retained in the catalog: Chain Lightning
and Dissonant Whispers upcasting, Clairvoyance and Legend Lore casting times,
Jump's material, Command's Approach option, Knock's targeting paragraph, and
"up to" durations. The original 178-spell review is extended to 219 spells for feat grants. The mechanics review and compatibility
matrix are in [SPELL_RULES_AUDIT.md](SPELL_RULES_AUDIT.md).
Species-associated spells are also bundled from the same SRD transcription.
Species spell-grant names, unlock levels, and casting limits follow the
[2024 Character Origins rules](https://www.dndbeyond.com/sources/dnd/br-2024/character-origins)
and [2024 Aasimar reference](https://roll20.net/compendium/dnd5e/Races%3AAasimar?expansion=32231).
Species flavor text and full PHB species descriptions are not reproduced.

The following PHB-only spells have original descriptions summarizing their rules,
checked against each spell's licensed 2024 Roll20 compendium entry:

- [Arcane Gate](https://roll20.net/compendium/dnd5e/Spells%3AArcane%20Gate?expansion=32231)
- [Arcane Vigor](https://roll20.net/compendium/dnd5e/Spells%3AArcane%20Vigor?expansion=32231)
- [Arms of Hadar](https://roll20.net/compendium/dnd5e/Spells%3AArms%20of%20Hadar?expansion=32231)
- [Blade Ward](https://roll20.net/compendium/dnd5e/Spells%3ABlade%20Ward?expansion=32231)
- [Cloud of Daggers](https://roll20.net/compendium/dnd5e/Spells%3ACloud%20of%20Daggers?expansion=32231)
- [Crown of Madness](https://roll20.net/compendium/dnd5e/Spells%3ACrown%20of%20Madness?expansion=32231)
- [Friends](https://roll20.net/compendium/dnd5e/Spells%3AFriends?expansion=32231)
- [Hunger of Hadar](https://roll20.net/compendium/dnd5e/Spells%3AHunger%20of%20Hadar?expansion=32231)
- [Mind Sliver](https://roll20.net/compendium/dnd5e/Spells%3AMind%20Sliver?expansion=32231)
- [Summon Aberration](https://roll20.net/compendium/dnd5e/Spells%3ASummon%20Aberration?expansion=32231)
- [Summon Construct](https://roll20.net/compendium/dnd5e/Spells%3ASummon%20Construct?expansion=32231)
- [Synaptic Static](https://roll20.net/compendium/dnd5e/Spells%3ASynaptic%20Static?expansion=32231)
- [Thunderclap](https://roll20.net/compendium/dnd5e/Spells%3AThunderclap?expansion=32231)
- [Witch Bolt](https://roll20.net/compendium/dnd5e/Spells%3AWitch%20Bolt?expansion=32231)

Spirit mechanics were checked against
[Aberrant Spirit](https://roll20.net/compendium/dnd5e/Monsters%3AAberrant%20Spirit?expansion=32231),
[Construct Spirit](https://roll20.net/compendium/dnd5e/Monsters%3AConstruct%20Spirit?expansion=32231), and
[Draconic Spirit](https://roll20.net/compendium/dnd5e/Monsters%3ADraconic%20Spirit?expansion=32231).

To rebuild the database, edit the reviewed UTF-8 catalog in
`scripts/data/spell-catalog.tsv` and run `scripts/Generate-SpellDetails.ps1`.
The catalog preserves the reviewed transcription corrections and original PHB
summaries; rebuilding does not download or replace them with an upstream version.
The bundled database is included so ordinary builds and launches work offline
using the same SQLite JDBC dependency as character saves.

The Metamagic descriptions in `domain/character/Metamagic.java` are original concise
summaries checked against the [2024 Sorcerer class](https://roll20.net/compendium/dnd5e/Classes%3ASorcerer?expansion=32231).
Rest recovery, Innate Sorcery, and progression were checked against the
[2024 Basic Rules class chapter](https://www.dndbeyond.com/sources/dnd/br-2024/character-classes).
The casting tracker validates resources per cast. It does not own a turn clock or enforce the spell-slot-per-turn rule; apply that rule at the table.

Feat spell additions retain SRD attribution. Thirty-seven additional SRD descriptions come from the official Free Rules spell chapter, with metadata cross-checked against the SRD transcription. The remaining four (Toll the Dead, Word of Radiance, Compelled Duel and Thorn Whip) use original rules summaries checked against their licensed PHB 2024 references. Named Hideous Laughter/Floating Disk entries use their PHB names. Individual references are stored beside each record.
