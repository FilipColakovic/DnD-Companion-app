# Code structure and compatibility

The app has one shared character and five dashboard pages. UI components render state and dispatch actions; rules calculate results; persistence reads and writes snapshots.

## Character and rules

- `application/CharacterController` owns character name, Charisma, the shared session, save protection, save callbacks and the saved checkpoint. It has no Swing dependency.
- `domain/character/PlaySession` owns mutable resources and applies validated actions. `CastingRules` validates a read-only context before a successful cast spends resources and emits one session notification.
- `CharacterChoices` defines required creation choices. `CharacterGrants` derives effective proficiency grants; `SkillRules` calculates bonuses; `HitPointRules` calculates fixed HP and supported bonuses.
- `ChoiceIds` centralizes persisted identifiers and source scopes. `Proficiency` names skill tiers while retaining their saved numbers: 0, 1 and 2.
- `CharacterState`, `PlayState`, `CharacterSheet` and `SpellSelection` retain their save formats and selection APIs. Character and spell SQLite schemas are unchanged.

## Views

- `GrimoirePanel` composes navigation and pages. Its controller event updates the sidebar and appearance. The Character page uses the same controller rather than a hidden legacy screen.
- The Character page composes identity, vitals, abilities, skills, training and save components. `CharacterFormBindings` guards programmatic updates and binds inputs; existing components refresh in place.
- Spell Library composes filters, a measured card grid, reusable spell cards and a persistent inspector. Filters combine selected tiers and schools; an empty group means All. Spell descriptions live in the inspector and Full details window.
- Cast & Combat composes tier filters, spell cards, dossier, conditional dice tray, log and cast controls. Dice and resource calculations continue to use the domain rules.
- Feats retains its separate model, filters, cards and persistent inspector. Backgrounds and Metamagic use the shared session and existing source-aware choices.
- `AppStyle` owns the theme. `CardComponents` supplies configurable badges, level tiles, title buttons, metadata strips and card geometry. `TextUpdates` avoids unnecessary document replacement and preserves text selection.
- Views unsubscribe when disposed. The dashboard closes its controller after disposing its pages; the controller releases its session and selection subscriptions.

## Compatibility and deferred work

Use `GrimoirePanel.characterController()` for editing. `characterPanel()` remains a compatibility alias returning that nonvisual controller. The editing methods, checkpoint methods, combat selection methods and `SpellSelection` APIs remain available. The retired standalone Sorcerer and table-based spellbook UI classes are removed; callers should use dashboard pages. Prepared-spell links call `showPreparedSpells()` to open the filtered library.

Skill and choice adapters preserve existing property keys and values. Legacy HP, dormant spells, manual overrides, save protection and checkpoint restoration remain covered by checks. This cleanup does not add multiclassing, multiple characters, inventory automation or new feat/spell automation.

## Verification

Run `scripts/Verify.ps1` with JDK 25. It compiles current sources into a fresh `target/verification/classes` directory, copies resources, resolves SQLite JDBC from the existing download or Maven cache before downloading, and runs each check in a separate JVM. Desktop verification is the default. `-Headless` explicitly reports skipped desktop checks.

Checks use disposable databases. Logs and previews stay under `target/`; the real `data/character.db` is excluded. `ComponentRefreshChecks` renders Character, Spell Library, Cast & Combat and Feats at normal/narrow widths, both themes and text sizes 12–18, and checks stable widgets, drafts, caret selection and scroll state.

Java files use UTF-8 and four-space indentation, with explicit imports. `.editorconfig` provides editor defaults. Format code without changing behavior, and keep formatting separate from structural changes when preparing future commits.
