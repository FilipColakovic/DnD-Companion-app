# DnD Companion App

A Java 25 Swing companion with a character designation dashboard based on the supplied
HTML design: cream surfaces, black borders and mechanical shadows, beveled controls,
geometric icons, and yellow, red, and blue accents. It works offline.
The main window and modeless dialogs use a custom cream title bar with bevelled
window controls and a red close button. Drag the bar to move the window,
double-click it to maximize/restore, and drag its edges to resize. The main window
also has a minimize button. Closing still follows the character save/checkpoint rules.
The supplied Stitch icon is bundled in `src/main/resources/icons/` as PNG artwork
and its original SVG. Main and detail windows use it in the title bar and native
window/taskbar icons, with multiple resolutions for desktop scaling.
All pages and secondary windows share the theme and text-size setting. The sidebar
shows the character designation, HP, Sorcery Points, save DC, spell attack, free
Innate Sorcery uses, and a grid of level 1–9 spell slots. Click the HP tile to set
current and maximum HP; unset HP displays a dash. HP saves with the character,
restores with checkpoints, and recovers to the configured maximum on a Long Rest.
The active page is highlighted in yellow. Secondary windows have matching headers
and navigation back to the main workspace. Spell-detail windows open without a
sidebar or duplicate header, using large titles, coloured badges, a responsive
specifications grid, rules and scaling sections. Dark mode uses a charcoal palette;
new characters default to the cream theme. Existing saved display preferences remain available.
Spell selection, slot correction, Metamagic, casting, dice, concentration, and combat
logging use the same shared character.
Run `org.example.Main` in IntelliJ IDEA to open the window. No UI dependencies are required.
Reload the Maven project after updating so IntelliJ downloads the SQLite JDBC dependency.

The sheet's level control (1–20) and Charisma score (1–30) update all values immediately.
The Subclass selector offers only Aberrant Sorcery, Clockwork Sorcery, Draconic Sorcery,
and Wild Magic Sorcery from the 2024 Player's Handbook. Subclass bonuses start at level 3.
The dashboard fits the desktop with independently scrollable panels. Every unlocked
slot level is available; high levels do not resize the whole window unexpectedly.
The **Race / species** selector includes all ten PHB 2024 species. Elf, Gnome,
and Tiefling options include their spell-bearing lineages or legacies. Species spells
unlock at the appropriate character level, appear automatically in both spell windows,
and do not consume Sorcerer selection limits. The calculator includes these additional
cantrips and prepared spells; species grants do not add spell slots or Sorcery Points.
Spell-list labels identify species grants and their free casts per Long Rest.
Species spellcasting assumes Charisma. High Elf currently uses its starting
Prestidigitation cantrip; replacing that cantrip after a Long Rest is not tracked.
Human spells gained through an optional Origin feat are outside this species selector.
Sources: [2024 species rules](https://www.dndbeyond.com/sources/dnd/br-2024/character-origins)
and [2024 Aasimar](https://roll20.net/compendium/dnd5e/Races%3AAasimar?expansion=32231).
`GrimoirePanel` renders the dashboard; `CharacterSheetPanel` provides the sheet editor.
The reusable standalone `SorcererPanel` retains its existing public editing APIs.
`SorcererRules.calculate(level, charisma)` returns
an immutable Aberrant Sorcery snapshot. The overload `calculate(level, charisma, subclass)`
supports each of the four subclasses and can also be reused by another UI.

Use the sidebar's **Cast & Combat**, **Metamagic**,
**Spell Library**, and **Character** pages to move between tools. Character name,
level, species and subclass are real saved inputs. The top header is removed;
resources appear in the sidebar, with a separator above navigation. The combat
spell list fills the available height. Rest and Innate Sorcery controls are in Character.
**Spell Library** manages and inspects spells; **Cast & Combat** uses available
prepared spells and cantrips. There is no separate Prepared spells page. Check
**Prepared only** in the library to review chosen spells and automatic grants,
including dormant choices after lowering level. The Character page's **Prepared
spells** review link opens that filtered library and clears other filters. Unlearn
removes manual choices; automatic grants remain protected.
The main library follows the supplied card-and-codex design: compact search,
compact spell cards in two columns, and a plain draggable divider without a dotted border.
Cards keep the level, name, coloured badges, casting metadata and Learn/Unlearn
action; descriptions appear in the side inspector. Cards return to one column
when the list is narrow or larger text needs more room. Rows use the height of
their own pair rather than the tallest spell in the entire catalog.
counted tier tabs, school filters, availability/prepared/Concentration/Ritual filters,
a continuous scrollable list of all matching spells. **Psionic** filters Aberrant Sorcery subclass
grants, including Mind Sliver. Click anywhere on a spell card to update the inline inspector; locked spells can
still be inspected. Cards show **Learn**, **Unlearn**, or **Always prepared**.
Learning uses the existing cantrip/prepared-spell limits; automatic grants cannot
be removed. Locked spells have a disabled Learn button. Casting stays in Cast & Combat. The inspector footer contains
only **Full details**, which opens the existing complete spell window.
Tier and school buttons allow multiple selections; selected values within a group
combine, while tier and school groups intersect. Click a selected button to deselect
it. With no selections in a group, **All** is active. **All** also clears that group.
Spells are ordered by level and name; the sort dropdown and Reset button are removed. Compatible chosen Metamagic appears as
inspector chips with eligibility tooltips. Resource updates reuse cards and preserve
the inspector and its scroll position. The list and inspector stack in narrow windows.
The library lists all 150 PHB 2024
Sorcerer spells (including 20 cantrips), plus the selected
subclass's additional spells. Search by name, filter by spell level, or show only
available spells / your selected spells. Check the Choose column to select spells;
the counters track class cantrips and prepared level 1+ spells separately.
Subclass spells unlock automatically and do not consume class selection limits.
The spell window follows changes to level and subclass in the main window.
The **My spells** button at the top right opens a separate window with your
known cantrips and prepared spells, including automatic subclass grants. It updates
live as choices, level, or subclass change. Search, spell-level filters, and spell
detail clicks work there too. Dormant choices show that a higher level is required.
Checkbox changes in the full list update individual cells without resetting the
scroll position, row selection, or wrapped row heights.
Use **Prepared only** in Spell Library to review this list in the main window.
Each spell has a short summary. Rows grow to fit wrapped text and adjust when the
window or column is resized. Click its name (or select a row and press Enter)
to open another app window with the spell's school, casting time, range, components,
duration, Concentration/Ritual flags, full effects, and scaling. Summoning spells
also include the relevant spirit stat block. Clicking the checkbox only changes
your choices. Spell data is bundled locally; no browser or runtime internet access
is required. Sources and licensing are in [SPELL_DATA_LICENSE.md](SPELL_DATA_LICENSE.md).

Spell names, levels, class-list membership, descriptions, casting details, scaling,
flags and reference URLs are bundled in `src/main/resources/spells/spells.db`.
`SpellRepository` separates these records from their SQLite implementation. The
repository opens the catalog read-only and caches all entries once; normal list
and detail clicks read from memory. It loads from application resources, including
a packaged JAR, and does not depend on the launch directory. Character data stays
in `data/character.db`; spell catalog updates require no character-save migration.
Rules calculations, subclass/species grants, dice and Metamagic logic remain in Java.

To edit spell data, update `scripts/data/spell-catalog.tsv` (UTF-8, one spell per
line; tabs separate columns, with `\n`, `\r`, `\t` and `\\` escapes inside fields).
Rebuild the bundled catalog offline using the existing JDK and SQLite driver:

```powershell
./scripts/Generate-SpellDetails.ps1
```

The script accepts `-Java`, `-DriverPath` and `-SourcePath` overrides. It validates
the source and preserves the existing database if generation fails. Maven and
IntelliJ copy the bundled database as a normal application resource. For manual
compilation, copy resources as shown below. The database is committed with the
source, so ordinary launches do not need to rebuild it.

Character name, level, Charisma, subclass, species/lineage, and manual spell choices are saved to the local SQLite file
`data/character.db` when you close the main window, and restored on the next launch.
The file is relative to the application's working directory (the project root in IntelliJ)
and is excluded from Git. Saving replaces the previous character in a single transaction.
If saving fails, the app displays an error and keeps the window open to allow another attempt.
Subclass and species spell grants and all calculated stats are derived from the saved inputs.
Closing just the spell window retains your choices in memory; close the main window to save.
The first launch defaults to level 1, Charisma 17, Aberrant Sorcery, and Human.
Existing saves are migrated automatically, preserving inputs and choices and defaulting species to Human.
Spent spell slots, Sorcery Points, rest recovery usage, Metamagic choices, species free
casts, concentration, Innate Sorcery, theme and text size are saved in the
same transaction. Older saves start with full resources and no Metamagic choices.

Check **Lock saved character** in the main window to save the current inputs and spell
choices immediately and protect that checkpoint. You can then experiment with levels,
Charisma, subclasses, species, and spells. Closing the app while locked keeps the
checkpoint and discards those experiments. **Restore saved** returns to the checkpoint
immediately and updates any open spell windows. The lock remains checked across app
restarts. Uncheck it to resume normal saving of the current character on exit; unchecking
does not itself replace the checkpoint. A failed checkpoint save leaves protection unchanged.
The status label shows **Saved**, **Changes will save on exit**, or **Experimenting ·
save protected**. Resources, Metamagic, and appearance settings follow the same lock.

Spell-slot and Sorcery Point controls display **remaining / maximum**. Use minus to
spend and plus to correct a count. **Long Rest** refills slots and points, resets daily
uses and rest recovery, and ends concentration and Innate Sorcery. **Short Rest**
leaves slots unchanged; from level 5, Sorcerous Restoration recovers half your level
(rounded down) in points, once per Long Rest. Rest buttons record a completed rest;
they do not simulate its duration. Changing levels preserves spent resources.

**Feats** opens a PHB 2024 compendium with all 75 feats, compact searchable cards,
and a **Backgrounds** tab with the sixteen PHB 2024 backgrounds. Select one there
to grant its fixed skills and Origin feat. Required skill, tool, ability-allocation
and equipment decisions appear on Character. A small red **!** beside Character
stays visible while any active decision is pending and clears after confirmation.
Background ability allocations and equipment are recorded; the ability fields
hold your final scores, and inventory remains manual.
The feat catalog has counted category tabs and ability-increase filters. Cards are grouped into Equipped,
Available and Locked sections, with alphabetical names within each category.
The library occupies 60% of the wide layout and stacks above the inspector on
narrow screens. Inspect
prerequisites, benefits, matching prepared spells, and upcoming level milestones.
Expand **Choices & notes** or **Milestone preview** for editing and simulation.
Resource updates retain the focused inspector, notes drafts, cursor, scroll position
and preview level. Restoring the saved checkpoint discards unsaved notes drafts.
Use **Equip feat** to record a choice and **Save feat notes** to record its ability,
spell, skill or other configuration. Selected feats also appear at the bottom of
Character info. Feat choices and notes use the existing SQLite checkpoint and save
lock; old saves load with no selected feats. Milestone/Charisma previews do not
change the character. Configured skill grants and expertise apply automatically;
ability increases, granted spells, other passive
bonuses and feat resources are recorded on your sheet rather than automatically
applied by this panel. Confirm prerequisites for scores or training the app does
not track. Fighting Style feats remain locked for a single-class Sorcerer.
**Full PHB entry** opens the licensed 2024 reference; **Print feat dossier** prints
the selected feats and notes. The catalog uses original concise summaries of the
[PHB 2024 feat list](https://roll20.net/compendium/dnd5e/Rules%3AFeat%20Descriptions?expansion=32231)
and links each feat to its individual 2024 entry.

**Character & Sheet** follows the supplied three-column sheet design and includes
editable identity, level, subclass, species, background, alignment, all six ability
scores, 18 skill proficiency/expertise selectors, saving throw displays and passive
senses. Charisma and level remain synchronized with spellcasting. Other abilities
start at 10 for older saves; enter your actual scores. Skill tiers start untrained.
Pending PHB 2024 creation choices appear as reminder cards. Select exactly the
required number of options and press **Select** to hide that reminder. Use
**Review saved choices** to edit confirmed selections. Sorcerer skills, species
skills and configured Skilled grants update skill bonuses and passive senses;
lineage spellcasting ability updates spell attacks and save DCs. Human Origin
feats and their required spell/tool choices are recorded with the character.
See [the coverage and rule references](CHARACTER_CHOICES.md) for every species,
source-aware proficiency behavior and the limits of feat automation.
Constitution and Charisma saves include Sorcerer proficiency. Extra feat-derived
save proficiencies and ability bonuses still require manual sheet configuration.
AC defaults to 10 + Dexterity modifier; set the AC override for armor or other
calculations. Speed, languages, training and equipment are editable inputs.

The vitals pod calculates maximum HP from Sorcerer level and Constitution using 6 + CON at level 1 and 4 + CON for later levels (minimum one HP gained). Automatic mode includes Dwarven Toughness, Draconic Resilience from level 3, Tough and Boon of Fortitude from level 19. It preserves damage taken; zero HP remains zero until healed. Unset HP defaults to calculation, while existing manual maxima remain manual. Toggle **Calculate maximum HP from class** to switch modes; use manual mode for rolled HP. The formula is shown beneath the HP controls.

The vitals pod tracks current/maximum/temporary HP, available d6 Hit Dice, manual
death-save markers and Heroic Inspiration. **Spend d6 Hit Die** records Short Rest
healing with the Constitution modifier, a minimum of 1 HP, and a cap at maximum HP. **Long Rest**
also restores Hit Dice and clears temporary HP and death-save markers. Sheet skill
names roll checks into the combat log. Slot and Sorcery Point correction controls
remain available. Class/species spell grants and chosen Metamagic are linked from
the features section; selected feats remain at the bottom of the page.

**Commit to grimoire** saves immediately through the same SQLite transaction as
window closing. It stays disabled while save protection is enabled. **Discard edits**
restores the saved character and sheet; **Export sheet** opens the print dialog.
Additional sheet inputs are stored inside the existing play-state properties, so
older SQLite files load without a schema migration. Multiple-character rosters,
other classes and automatic feat effects remain deferred. Calculation references:
[2024 proficiency and skills](https://www.dndbeyond.com/sources/dnd/br-2024/playing-the-game),
[Sorcerer traits](https://www.dndbeyond.com/sources/dnd/br-2024/character-classes#Sorcerer),
and [rest rules](https://www.dndbeyond.com/sources/dnd/br-2024/rules-glossary).

**Metamagic** shows all ten PHB 2024 options as cards with SP costs, rules notes,
live compatible prepared-spell chips, and **Select / Equipped** buttons. The grid
adapts from three columns to one for narrow windows and larger text. Select two
from level 2, four from level 10, and six from level 17. Click an option's name for its
description and a compatible-spell list. This list starts collapsed; click **Show
compatible spells** to expand it. Click a spell to open full details. Lists identify
candidates using bundled spell effects; target choices and situational conditions
still apply. Summoned creatures' attacks are excluded; Wish's replicated effects
require a table ruling. Lowering your level retains choices and flags excess options.

**Cast & Combat** uses your selected cantrips and prepared spells, including
available automatic grants. Click a spell card and filter the arsenal by level.
The dossier shows casting time, range, components, save DC, attack modifier,
concentration, effects and damage. **Cast** spends a base-level spell slot;
cantrips spend no slot. Invalid casts spend nothing. Replacing concentration
requires confirmation. Casting can be repeated while appropriate resources remain.
Turn timing and action economy are managed at the table.

The removed slot, attack/save, concentration and Metamagic panels remain removed.
The current page displays compatible chosen Metamagic as hints; it has no controls
for applying Metamagic, upcasting or species free casts. Their domain rules and
saved resources remain supported. Empty characters have a **Choose spells** route
to the spell library. Targets, action economy and material requirements are
resolved at the table. Attack and damage rolls remain available in the contextual
dice tray. Hidden UI controls have been replaced by plain casting selection state.
Spell cards and filter controls are reused during resource updates.

Twinned uses an explicit additional-target upcast list; Chromatic Orb's conditional
leaps, extra rays/darts and larger areas are excluded. Damage summaries distinguish
primary rolls, mixed effects and incidental damage: Web shows restraint and the
separate ignition condition rather than presenting 2d4 as its casting damage.
The complete spell and compatibility review is in [SPELL_RULES_AUDIT.md](SPELL_RULES_AUDIT.md).
The dice tray appears only when the selected spell has caster dice to roll, showing
only the relevant die types. Hold Person, Friends, and other save-only effects hide it;
Scorching Ray shows d20 and d6, while Fireball shows d6 without a target-save d20.
Attack d20 rolls add the spell attack modifier and use Innate Sorcery Advantage.
Blink, Mirror Image, False Life, and other caster effect rolls show their own dice;
Teleport includes d100 for its arrival table. Target penalties and summon stat blocks
do not add caster dice. Quick damage rolls use the selected spell's supported dice
formula. Critical rolls double damage dice. Complex or weapon-dependent profiles
require the spell text rather than an assumed formula. Casts, resource changes and rolls populate the live
combat log; **Clear** clears it. The spell-level filters combine selected tiers;
click a selected tier again to remove it. With none selected, **All** is active.
The spell dossier expands to fill unused vertical space.
The duplicate header statistics and combat turn/point bar are removed; resources appear
in the sidebar designation panel. Space/Enter
casts from the combat view when keyboard focus is outside text/selector/button controls.
The combat log lasts for the current session; resource counts are restored after
reopening. Former turn flags in older saves are ignored and omitted from new saves.
The app does not track turns, including the level 20 Metamagic limit per turn;
its free option is calculated independently for each casting while Innate Sorcery is active.

**Innate Sorcery** toggles the feature and tracks its two free uses per Long Rest;
from level 7, further activations cost two points. While active, the casting preview
includes its Sorcerer-spell benefits. Metamagic application controls remain deferred;
their level 7 and level 20 rules are retained in the casting model. End Innate Sorcery
manually after one minute.
**Dark theme** and **Text size** apply across open windows and are saved with your
character. Rules references: [2024 Sorcerer and Metamagic](https://roll20.net/compendium/dnd5e/Classes%3ASorcerer?expansion=32231),
[class progression](https://www.dndbeyond.com/sources/dnd/br-2024/character-classes),
and [2024 spellcasting](https://www.dndbeyond.com/sources/dnd/br-2024/spells).

Changes to level/subclass preserve choices. Locked choices
remain checked but do not count as currently available. If a lower level leaves too
many active choices, the selector flags the limit and allows you to remove them.
This is a character builder; it does not enforce the history of spell replacements
between level-ups.

Calculations:

- Charisma modifier = floor((Charisma − 10) / 2), including negative modifiers.
- Proficiency bonus = 2 + floor((Sorcerer level − 1) / 4), for a single-class character.
- Spell save DC = 8 + proficiency bonus + Charisma modifier.
- Spell attack = proficiency bonus + Charisma modifier.
- Cantrips, prepared spells, and spell slots use the 2024 class progression.
- Sorcery Points are unavailable at level 1, then equal Sorcerer level.
- Aberrant Sorcery adds 4/6/8/10 prepared level 1+ spells at levels 3/5/7/9,
  plus Mind Sliver from level 3. Cantrips are counted separately from prepared level 1+ spells.
- Clockwork and Draconic Sorcery add the same number of prepared level 1+ spells,
  without an additional cantrip. Wild Magic Sorcery adds no prepared spells or cantrips.

Counts assume class spell choices do not overlap subclass or species grants, including Mind Sliver.
It excludes multiclassing, feats, items, slot conversion, and subclass-specific
resource costs. The sidebar and casting preview include active Innate
Sorcery for Sorcerer spells; the character editor shows base DC and attack values.

Sources: [2024 Sorcerer class](https://www.dndbeyond.com/classes/2190884-sorcerer),
[2024 Aberrant Sorcery](https://roll20.net/compendium/dnd5e/Subclasses%3AAberrant%20Sorcery?expansion=32231),
[2024 Clockwork Sorcery](https://roll20.net/compendium/dnd5e/Subclasses%3AClockwork%20Sorcery?expansion=32231),
[2024 Wild Magic Sorcery](https://roll20.net/compendium/dnd5e/Subclasses%3AWild%20Magic%20Sorcery?expansion=32231),
[2024 Draconic Sorcery](https://roll20.net/compendium/dnd5e/Subclasses%3ADraconic%20Sorcery?expansion=32231),
[PHB 2024 Sorcerer spell list](https://roll20.net/compendium/dnd5e/Classes%3ASorcerer?expansion=32231).

Code is organized under `src/main/java/org/example/`:

- `Main.java`: application launcher; its run configuration stays `org.example.Main`.
- `domain/character`: Sorcerer rules, calculated stats, resources and immutable sheet inputs.
- `persistence`: character SQLite storage and the separate read-only spell repository.
- `domain/spells`: repository interface, spell catalog APIs, rules and character spell choices.
- `src/main/resources/spells/spells.db`: bundled spell metadata and details.
- `ui/character`: sheet editor, Feats filters/cards/inspector, Metamagic and standalone calculator.
- `ui/GrimoirePanel.java`: sidebar, navigation, character profile and checkpoints.
- `ui/spells`: spellbook, filtering/table model, wrapping row renderer, and detail window.
- `ui/spells/CastingPanel.java`: casting selection, contextual dice, spell annotations and combat log.
- `ui/common`: shared styling and icons.

Regression checks mirror the UI packages under `src/test/java/org/example/`.
The offline spell-data builder in `scripts/` writes the bundled SQLite catalog.

To compile and run in PowerShell with JDK 25 on PATH:

```powershell
New-Item -ItemType Directory -Force target/classes | Out-Null
New-Item -ItemType Directory -Force target/dependency | Out-Null
Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0.jar' -OutFile target/dependency/sqlite-jdbc-3.53.4.0.jar
$sources = @(Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName)
javac -encoding UTF-8 -d target/classes $sources
Copy-Item -Recurse -Force src/main/resources/* target/classes/
java --enable-native-access=ALL-UNNAMED -cp 'target/classes;target/dependency/*' org.example.Main
```

The database uses the [Xerial SQLite JDBC driver](https://github.com/xerial/sqlite-jdbc).
No database server is required. Downloading the driver is a one-time setup step.

Run the regression checks (the persistence checks use the same driver):

```powershell
New-Item -ItemType Directory -Force target/test-classes | Out-Null
$tests = @(Get-ChildItem -Recurse src/test/java -Filter *.java | ForEach-Object FullName)
javac -encoding UTF-8 -cp target/classes -d target/test-classes $tests
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.character.SorcererChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.spells.SpellBookChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.spells.SpeciesChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.character.PlayChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.domain.spells.DamageDiceChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.domain.spells.SpellRulesChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.GrimoireChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.spells.DiceTrayChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.spells.SpellLibraryChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.character.MetamagicCardChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.character.FeatsChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.ui.character.CharacterSheetChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.BauhausChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=true' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.persistence.CharacterStoreChecks
java --enable-native-access=ALL-UNNAMED -cp 'target/classes;target/test-classes;target/dependency/*' org.example.persistence.SpellRepositoryChecks
java --enable-native-access=ALL-UNNAMED '-Djava.awt.headless=false' -cp 'target/classes;target/test-classes;target/dependency/*' org.example.WindowPersistenceChecks
```

Checks cover all levels, ability modifiers, boundary validation, slider/button updates,
spell-list counts, subclass grants, selection limits, retained choices, search, filtering,
detail clicks, data completeness, readable subclass menu selections, wrapped descriptions
at multiple column widths and font sizes, and live level/subclass synchronization from the main window.
They also render calculator and spellbook previews in `target/` for visual inspection.
Run `SorcererChecks` with `'-Djava.awt.headless=false'` to also verify automatic frame growth.
Run `GrimoireChecks` with `'-Djava.awt.headless=false'` to verify real dialogs and the
compact dashboard layout at level 20 with larger text. It renders dark, light and
compact dashboard previews in `target/`.
Database checks use disposable files under `target/`. They verify save/reopen, replacing
spell choices, preserving dormant choices, atomic rollback after a write failure,
UI restoration, and the real main-window close handler.
They also verify checkpoint creation, protecting experiments, restoration in open spell
windows, locked exit/reopen, and resuming normal saves after unlocking.
