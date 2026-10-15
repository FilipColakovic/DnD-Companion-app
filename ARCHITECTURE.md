# Code structure and compatibility

The app starts on an offline character roster and keeps one active character controller/session. UI components render state and dispatch actions; rules calculate results; persistence reads and writes snapshots.

## Application and storage

- `ApplicationController` owns startup, roster actions, navigation and window lifetime. Its serial worker performs database/file work off Swing's event thread, capturing explicit UUIDs. A successful transition disposes the previous dashboard; a failed save/load retains it.
- `CharacterRepository` and `SQLiteCharacterRepository` store independent snapshots, locks, archive/campaign metadata and timestamps in `data/characters.db`. Legacy import uses a read-only SQLite backup, imports once in a transaction and never edits `data/character.db`.
- `CharacterJson` uses pinned Jackson 3 with an explicit version-one format and validated single-class data. Imports create new IDs. Copies retain gameplay state and configuration, with a new name/ID and cleared lock/archive flags. Appearance preferences belong to `app_settings`, not imported files.
- `Main` starts the controller. Its small `createWindow` adapter and `LegacyWindowFactory` remain only for older single-save window checks.

## Character and rules

- `application/CharacterController` owns character editing, the shared session, save protection, callbacks and checkpoint. All six base abilities live in `CharacterSheet`; the old Charisma argument remains a serialization adapter. It has no Swing dependency.
- `domain/character/PlaySession` owns mutable resources and applies validated actions. `CastingRules` validates a read-only context before a successful cast spends resources and emits one session notification.
- `FeatEffects` derives capped ability increases, repeatable choices and source-specific spell grants without changing base scores. `FeatActions` defines triggered actions/pools; `FeatActionService` stages changes before the session commits them. `FeatCombatRules` and `EquipmentRules` handle player-confirmed equipment/circumstances. `HealingDice` applies supported healing formulas and Healer rerolls.
- `CharacterChoices` defines required creation choices. `CharacterGrants` derives effective proficiency grants; `SkillRules` calculates bonuses; `HitPointRules` calculates fixed HP and supported bonuses.
- `ChoiceIds` centralizes persisted identifiers and source scopes. `Proficiency` names skill tiers while retaining their saved numbers: 0, 1 and 2.
- `CharacterGrants.sources` identifies class/species/background/feat-copy grants. New proficiency-icon upgrades persist dependencies under `upgrade.<SKILL>`; dormant sources grant nothing. Legacy manual tiers remain independent. Background ability allocations apply automatically to newly created characters; legacy allocations remain recorded without double-counting existing scores.
- `ClassId`, `ClassProgression`, `ClassDefinition` and `ClassRules` separate identities, progression, training and mechanics. Sorcerer delegates existing tables through `SorcererClassRules`; Fighter/Champion uses `FighterRules`, `ClassActions`, staged `ClassActionService` and pure `ClassResourceRules`. Named `ResourcePool`s expose recovery rules; existing Sorcerer fields and the old `Stats` type remain compatibility adapters. Fighter spent pools use namespaced play counters. Rules follow the [2024 class chapter](https://www.dndbeyond.com/sources/dnd/br-2024/character-classes).
- `CharacterState`, `PlayState`, `CharacterSheet` and `SpellSelection` retain legacy encoding/selection adapters. Class progression is a one-element collection stored under `choice.class.progression`; multiclass entries are rejected. The roster schema is separate from the untouched legacy database.
- The read-only bundled spell database uses schema 2 with normalized class-list membership and explicit attack/save metadata. `SpellProfile` supplies mechanics to the UI, replacing attack/save detection from prose. Class rules supply preparation/progression/casting ability; species and feat casting sources remain independent.

## Views

- `GrimoirePanel` composes navigation and pages. Its controller event updates the sidebar and appearance. The Character page uses the same controller rather than a hidden legacy screen.
- The Character page composes identity, vitals, abilities, skills, training and save components. `CharacterFormBindings` guards programmatic updates and binds inputs; existing components refresh in place.
- Spell Library composes filters, a measured card grid, reusable spell cards and a persistent inspector. Filters combine selected tiers and schools; an empty group means All. Spell descriptions live in the inspector and Full details window.
- Cast & Combat composes tier filters, spell cards, dossier, compatible Metamagic checkboxes, conditional dice tray, log and cast controls. Per-cast Metamagic selections survive resource refreshes and reset when changing spells; the domain validates combinations and spends resources atomically.
- Feats retains its separate model, filters, cards and persistent inspector. Backgrounds and Metamagic use the shared session and existing source-aware choices.
- `AppStyle` owns the theme. `CardComponents` supplies configurable badges, level tiles, title buttons, metadata strips and card geometry. `TextUpdates` avoids unnecessary document replacement and preserves text selection.
- Views unsubscribe when disposed. The dashboard closes its controller after disposing its pages; the controller releases its session and selection subscriptions.

## Compatibility and deferred work

Use `GrimoirePanel.characterController()` for editing. `characterPanel()` remains a compatibility alias returning that nonvisual controller. The editing methods, checkpoint methods, combat selection methods and `SpellSelection` APIs remain available. The retired standalone Sorcerer and table-based spellbook UI classes are removed; callers should use dashboard pages. Prepared-spell links call `showPreparedSpells()` to open the filtered library.

Legacy HP, dormant spells, manual overrides, save protection and checkpoint restoration remain covered by checks. Battle Master, Eldritch Knight and Psi Warrior are separate future increments, followed by Wizard, Cleric and the remaining PHB classes. Multiclassing, accounts/sync and full inventory/encounter automation remain deferred. Combat circumstances are player-confirmed; there is no Next Turn engine. See [FEAT_AUTOMATION.md](FEAT_AUTOMATION.md).

## Verification

Run `scripts/Verify.ps1` with JDK 25. It compiles into a fresh `target/verification/classes` directory, copies resources, resolves pinned SQLite/Jackson dependencies from the existing download or Maven cache before downloading, and runs each check in a separate JVM. Desktop verification is the default. `-Headless` explicitly reports skipped desktop checks.

Checks use disposable databases. Logs and previews stay under `target/`; real saves are excluded. `RosterStoreChecks` covers migration, IDs, locks, archives and JSON. `RosterWindowChecks` exercises asynchronous navigation, protected experiments and failed writes. `FeatRemovalChecks`, `FighterChecks` and real Character controls cover source removal and class mechanics. Existing layout/refresh checks cover normal/narrow widths, both themes, text sizes 12–18, stable widgets, drafts, caret selection and scroll state.

Java files use UTF-8 and four-space indentation, with explicit imports. `.editorconfig` provides editor defaults. Format code without changing behavior, and keep formatting separate from structural changes when preparing future commits.
