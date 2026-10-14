# 2024 spell and Metamagic review

Original review 2026-10-10: 178 Sorcerer, subclass and species entries. Extended 2026-10-14 to 219 entries for feat-granted spells,
and all ten Metamagic options. This table records the implemented candidates;
targets, actual rolls and conditions still determine whether an option can be used.

164 descriptions were compared with the official [SRD 5.2.1 spell chapter](https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf).
The other 14 original summaries were checked against their licensed PHB 2024
references linked in [SPELL_DATA_LICENSE.md](SPELL_DATA_LICENSE.md).
[The 2024 Sorcerer Metamagic rules](https://roll20.net/compendium/dnd5e/Classes%3ASorcerer?expansion=32231)
define the ten options. SRD attribution remains in SPELL_DATA_LICENSE.md.

Corrections retained in the generator: missing Chain Lightning and Dissonant
Whispers upcasting, Clairvoyance/Legend Lore casting times, Jump's material,
Command's Approach option, Knock's target definition, "up to" durations, and a
Wall of Stone transcription typo. Remaining textual comparison differences are
table formatting. PHB spell flavor text is not copied.

Damage automation uses reviewed single-component profiles, not the first dice
found in a description. Web's ignited-web damage, teleportation mishaps,
collapsing structures, weapon attacks, summoned-creature attacks and multipart
damage receive separate explanatory notes. Single-component rolls identify their
timing and scope (per ray, dart, hit, save or damage trigger).

Twinned uses explicit additional-target upcasting. Chromatic Orb's conditional
leaps, Magic Missile darts, Scorching Ray rays, larger areas and stronger summoned
attacks are excluded from the automatic candidate list. Chain Lightning's explicit
extra target bolt and Etherealness's additional willing travelers are included.
Wish's replicated effects require the chosen spell's rules, so its base entry is
not treated as a universal candidate. Empowered/Seeking require actual damage
dice/a missed caster attack. Transmuted changes only a listed elemental component;
Sorcerous Burst's Psychic choice cannot be transmuted. Creation's material-dependent
duration qualifies for Extended; a duration at/above 24 hours cannot increase.

The table uses base spell level and character level 1 for cantrip damage. Slots
and character levels update supported formulas in the app. Candidate conditions
appear in casting tooltips and the Metamagic detail list. Candidates do not bypass
Sorcery Point, combination or slot restrictions. Turn rules remain player-managed.

The extension includes complete Magic Initiate, Fey/Shadow-Touched and Ritual Caster choice data. Feat range/component overrides, healing rerolls and chosen damage types are derived separately; published descriptions remain intact. See [FEAT_AUTOMATION.md](FEAT_AUTOMATION.md).

| Spell | Damage annotation | Metamagic candidates |
| --- | --- | --- |
| Acid Splash | Damage roll: 1d6 Acid · failed save, per creature. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Blade Ward | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Chill Touch | Damage roll: 1d10 Necrotic · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle |
| Dancing Lights | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Druidcraft | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Eldritch Blast | Resolve damage timing, targets and conditions from the spell text. | Distant, Empowered, Quickened, Seeking, Subtle |
| Elementalism | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Fire Bolt | Damage roll: 1d10 Fire · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Friends | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Guidance | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Light | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Mage Hand | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Mending | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Message | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Mind Sliver | Damage roll: 1d6 Psychic · failed save. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Minor Illusion | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Poison Spray | Damage roll: 1d12 Poison · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Prestidigitation | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Produce Flame | Damage roll: 1d8 Fire · on a hit. Follow the spell text for additional effects. | Empowered, Extended, Seeking, Subtle, Transmuted |
| Ray of Frost | Damage roll: 1d8 Cold · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Resistance | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Sacred Flame | Damage roll: 1d8 Radiant · failed save; target gains no cover benefit. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Shillelagh | No direct damage. Apply the spell's described effects. | Extended, Subtle |
| Shocking Grasp | Damage roll: 1d8 Lightning · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Sorcerous Burst | Damage roll: 1d8 chosen type (exploding) · on a hit; extra dice capped by spellcasting modifier. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Spare the Dying | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Starry Wisp | Damage roll: 1d8 Radiant · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle |
| Thaumaturgy | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Thorn Whip | Damage roll: 1d6 Piercing · on a hit; pull eligible target up to 10 feet. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle |
| Thunderclap | Damage roll: 1d6 Thunder · failed save. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Toll the Dead | Resolve damage timing, targets and conditions from the spell text. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| True Strike | Damage: weapon damage + spellcasting ability modifier. The spell replaces the weapon's normal ability modifier; choose Radiant or the weapon's normal type for its base damage. | Empowered, Quickened, Seeking, Subtle |
| Vicious Mockery | Resolve damage timing, targets and conditions from the spell text. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Word of Radiance | Damage roll: 1d6 Radiant · failed save, per chosen creature. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle |
| Alarm | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Animal Friendship | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Arms of Hadar | Damage roll: 2d6 Necrotic · failed save; half on success. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle |
| Bane | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Bless | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Burning Hands | Damage roll: 3d6 Fire · failed save; half on success. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Charm Person | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Chromatic Orb | Damage roll: 3d8 chosen type · per hit; leaps require matching dice. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Color Spray | No direct damage. Apply the spell's described effects. | Careful, Heightened, Quickened, Subtle |
| Command | No direct damage. Apply the spell's described effects. | Careful, Distant, Heightened, Quickened, Subtle, Twinned |
| Compelled Duel | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Subtle |
| Comprehend Languages | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Create or Destroy Water | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Cure Wounds | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Detect Evil and Good | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Detect Magic | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Detect Poison and Disease | No direct damage. Apply the spell's described effects. | Extended, Subtle |
| Disguise Self | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Dissonant Whispers | Damage roll: 3d6 Psychic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Divine Favor | No direct damage. Apply the spell's described effects. | Extended, Subtle |
| Divine Smite | No direct damage. Apply the spell's described effects. | Subtle |
| Ensnaring Strike | No direct damage. Apply the spell's described effects. | Extended, Subtle |
| Entangle | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Expeditious Retreat | No direct damage. Apply the spell's described effects. | Extended, Subtle |
| Faerie Fire | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| False Life | No direct damage. Apply the spell's described effects. | Quickened, Subtle |
| Feather Fall | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Find Familiar | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Fog Cloud | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Goodberry | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Grease | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Guiding Bolt | Damage roll: 4d6 Radiant · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle |
| Healing Word | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Hellish Rebuke | Damage roll: 2d10 Fire · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Subtle, Transmuted |
| Heroism | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Hex | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Hunter's Mark | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Ice Knife | Hit: 1d10 Piercing. Hit or miss: a Dexterity save against the explosion, 2d6 Cold on failure, plus 1d6 Cold per slot level above 1. | Careful, Distant, Empowered, Heightened, Quickened, Seeking, Subtle, Transmuted |
| Identify | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Illusory Script | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Inflict Wounds | Damage roll: 2d10 Necrotic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Jump | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle, Twinned |
| Longstrider | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Mage Armor | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Magic Missile | Damage roll: 1d4 + 1 Force · per dart. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Subtle |
| Protection from Evil and Good | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Purify Food and Drink | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Ray of Sickness | Damage roll: 2d8 Poison · on a hit. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| Sanctuary | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Subtle |
| Searing Smite | No direct damage. Apply the spell's described effects. | Extended, Subtle |
| Shield | No direct damage. Apply the spell's described effects. | Subtle |
| Shield of Faith | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Silent Image | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Sleep | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Speak with Animals | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Tasha's Hideous Laughter | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Tenser's Floating Disk | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Thunderwave | Damage roll: 2d8 Thunder · failed save; half on success. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Unseen Servant | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Witch Bolt | Initial hit: 2d12 Lightning + 1d12 per slot level above 1. Subsequent Bonus Action: 1d12 Lightning, even if the initial attack missed. | Distant, Empowered, Extended, Quickened, Seeking, Subtle, Transmuted |
| Aid | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Alter Self | Natural Weapons option changes your Unarmed Strike to 1d6 of the growth's damage type + spellcasting ability modifier. Other options do not deal damage. | Extended, Quickened, Subtle |
| Arcane Vigor | No direct damage. Apply the spell's described effects. | Subtle |
| Blindness/Deafness | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Blur | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Calm Emotions | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Cloud of Daggers | Damage roll: 4d4 Slashing · per damage trigger; once per creature per turn. Follow the spell text for additional effects. | Distant, Empowered, Extended, Quickened, Subtle |
| Crown of Madness | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Darkness | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Darkvision | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Detect Thoughts | No direct damage. Apply the spell's described effects. | Careful, Extended, Heightened, Quickened, Subtle |
| Dragon's Breath | Damage roll: 3d6 chosen type · per breath, failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Subtle, Transmuted |
| Enhance Ability | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Enlarge/Reduce | Changes the target's weapon and Unarmed Strike damage by +1d4 (Enlarge) or −1d4 (Reduce); this is not a separate spell damage roll. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Flame Blade | Melee spell attack: 3d6 Fire + spellcasting ability modifier; add 1d6 per slot level above 2. | Empowered, Extended, Seeking, Subtle, Transmuted |
| Flaming Sphere | Damage roll: 2d6 Fire · failed save, per trigger; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle, Transmuted |
| Gust of Wind | No direct damage. Apply the spell's described effects. | Careful, Extended, Heightened, Quickened, Subtle |
| Hold Person | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Invisibility | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Knock | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Lesser Restoration | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Levitate | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Magic Weapon | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Mind Spike | Damage roll: 3d8 Psychic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle |
| Mirror Image | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Misty Step | No direct damage. Apply the spell's described effects. | Subtle |
| Pass without Trace | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Phantasmal Force | Damage roll: 2d8 Psychic · per damaging phantasm turn; target must be in reach. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle |
| Ray of Enfeeblement | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Scorching Ray | Damage roll: 2d6 Fire · per ray. Follow the spell text for additional effects. | Distant, Empowered, Quickened, Seeking, Subtle, Transmuted |
| See Invisibility | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Shatter | Damage roll: 3d8 Thunder · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Spider Climb | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Suggestion | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Web | No damage on casting. Restrains creatures that fail a Dexterity save. Only webs ignited by a separate fire deal 2d4 Fire damage when a creature starts its turn in that fire. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Blink | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Clairvoyance | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Counterspell | No direct damage. Apply the spell's described effects. | Careful, Distant, Heightened, Subtle |
| Daylight | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Dispel Magic | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Fear | No direct damage. Apply the spell's described effects. | Careful, Extended, Heightened, Quickened, Subtle |
| Fireball | Damage roll: 8d6 Fire · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Fly | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Gaseous Form | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Haste | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Hunger of Hadar | Start of turn: 2d6 Cold. End of turn: Dexterity save, 2d6 Acid on failure. Add 1d6 per slot level above 3 to either damage type, chosen when casting. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle, Transmuted |
| Hypnotic Pattern | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Lightning Bolt | Damage roll: 8d6 Lightning · failed save; half on success. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Major Image | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Protection from Energy | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Sending | No direct damage. Apply the spell's described effects. | Quickened, Subtle |
| Sleet Storm | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Slow | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Stinking Cloud | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Tongues | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Vampiric Touch | Damage roll: 3d6 Necrotic · on a hit; heal half Necrotic damage dealt. Follow the spell text for additional effects. | Empowered, Extended, Quickened, Seeking, Subtle |
| Water Breathing | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Water Walk | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Arcane Eye | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Banishment | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Blight | Damage roll: 8d8 Necrotic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Charm Monster | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Confusion | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Dimension Door | Teleportation; no normal damage. A blocked arrival causes 4d6 Force damage to the travelers and the teleport fails. | Distant, Empowered, Quickened, Subtle |
| Dominate Beast | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Evard's Black Tentacles | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Fire Shield | A melee attacker within 5 feet takes 2d8 Fire (warm shield) or 2d8 Cold (chill shield). The spell also grants the opposite resistance. | Empowered, Extended, Quickened, Subtle, Transmuted |
| Freedom of Movement | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle, Twinned |
| Greater Invisibility | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Ice Storm | Dexterity save: 2d10 Bludgeoning + 4d6 Cold; half on success. Only Bludgeoning increases, by 1d10 per slot level above 4. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Polymorph | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Stoneskin | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Summon Aberration | The summoned creatures use their own stat-block attacks and effects. Their attacks are not the caster's spell attack or damage rolls. | Distant, Extended, Quickened, Subtle |
| Summon Construct | The summoned creatures use their own stat-block attacks and effects. Their attacks are not the caster's spell attack or damage rolls. | Distant, Extended, Quickened, Subtle |
| Vitriolic Sphere | Failed Dexterity save: 10d4 Acid now + 5d4 Acid at the end of the target's next turn. Success: half the initial damage only. Initial damage increases by 2d4 per slot level above 4. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Wall of Fire | Damage roll: 5d8 Fire · per damage trigger; initial save for half. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle, Transmuted |
| Animate Objects | The summoned creatures use their own stat-block attacks and effects. Their attacks are not the caster's spell attack or damage rolls. | Distant, Extended, Quickened, Subtle |
| Bigby's Hand | Clenched Fist: 5d8 Force on a spell attack hit (+2d8 per slot level above 5). Grasping Hand crush: 4d6 Bludgeoning + spellcasting ability modifier (+2d6 per higher slot). | Careful, Distant, Empowered, Extended, Heightened, Quickened, Seeking, Subtle |
| Cloudkill | Damage roll: 5d8 Poison · failed save, per trigger; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle, Transmuted |
| Cone of Cold | Damage roll: 8d8 Cold · failed save; half on success. Follow the spell text for additional effects. | Careful, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Creation | No direct damage. Apply the spell's described effects. | Distant, Extended, Subtle |
| Dominate Person | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Greater Restoration | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Hold Monster | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle, Twinned |
| Insect Plague | Damage roll: 4d10 Piercing · failed save, per trigger; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle |
| Legend Lore | No direct damage. Apply the spell's described effects. | Subtle |
| Rary's Telepathic Bond | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Seeming | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Summon Dragon | The summoned creatures use their own stat-block attacks and effects. Their attacks are not the caster's spell attack or damage rolls. | Distant, Extended, Quickened, Subtle |
| Synaptic Static | Damage roll: 8d6 Psychic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Telekinesis | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Teleportation Circle | No direct damage. Apply the spell's described effects. | Distant, Subtle |
| Wall of Force | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Wall of Stone | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Arcane Gate | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Chain Lightning | Damage roll: 10d8 Lightning · failed save, per target; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted, Twinned |
| Circle of Death | Damage roll: 8d8 Necrotic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Disintegrate | Damage roll: 10d6 + 40 Force · failed save. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Eyebite | No direct damage. Apply the spell's described effects. | Careful, Extended, Heightened, Quickened, Subtle |
| Flesh to Stone | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Globe of Invulnerability | No direct damage. Apply the spell's described effects. | Extended, Quickened, Subtle |
| Mass Suggestion | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Move Earth | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Otiluke's Freezing Sphere | Damage roll: 10d6 Cold · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Sunbeam | Damage roll: 6d8 Radiant · failed save, per beam; half on success. Follow the spell text for additional effects. | Careful, Empowered, Extended, Heightened, Quickened, Subtle |
| True Seeing | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Delayed Blast Fireball | Explosion: 12d6 Fire + 1d6 per slot level above 7 + 1d6 per turn ended while concentrating; Dexterity save for half. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle, Transmuted |
| Etherealness | No normal damage. Returning inside an occupied space causes Force damage equal to twice the distance shunted in feet. | Extended, Quickened, Subtle, Twinned |
| Finger of Death | Damage roll: 7d8 + 30 Necrotic · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Fire Storm | Damage roll: 7d10 Fire · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Plane Shift | No direct damage. Apply the spell's described effects. | Distant, Quickened, Subtle |
| Prismatic Spray | Roll the ray for each target. Red–blue deal 12d6 of the listed elemental type; other rays impose conditions. Resolve the Prismatic Rays table. | Careful, Empowered, Heightened, Quickened, Subtle, Transmuted |
| Reverse Gravity | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Teleport | Teleportation; no normal damage. A mishap causes 3d10 Force damage per traveler or object; further mishaps can repeat it. | Distant, Empowered, Quickened, Subtle |
| Demiplane | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Dominate Monster | No direct damage. Apply the spell's described effects. | Careful, Distant, Extended, Heightened, Quickened, Subtle |
| Earthquake | Structures take 50 Bludgeoning damage per tremor. Collapsing structures can deal 12d6 Bludgeoning damage to nearby creatures; falling into fissures is resolved separately. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle |
| Incendiary Cloud | Damage roll: 10d8 Fire · failed save, per trigger; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Extended, Heightened, Quickened, Subtle, Transmuted |
| Power Word Stun | No direct damage. Apply the spell's described effects. | Careful, Distant, Heightened, Quickened, Subtle |
| Sunburst | Damage roll: 12d6 Radiant · failed save; half on success. Follow the spell text for additional effects. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Gate | No direct damage. Apply the spell's described effects. | Distant, Extended, Quickened, Subtle |
| Meteor Swarm | Dexterity save: 20d6 Fire + 20d6 Bludgeoning, half on success; a creature in overlapping spheres is affected once. | Careful, Distant, Empowered, Heightened, Quickened, Subtle |
| Power Word Kill | A target with 100 HP or fewer dies; otherwise it takes 12d12 Psychic damage. | Distant, Empowered, Quickened, Subtle |
| Time Stop | No direct damage. Apply the spell's described effects. | Quickened, Subtle |
| Wish | Resolve the chosen Wish effect or replicated spell. Stress damage is a consequence of later casting, not Wish's primary damage. | Quickened, Subtle |
