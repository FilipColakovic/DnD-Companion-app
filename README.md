# DnD Companion App

Offline Java 25 desktop companion for PHB 2024 **Sorcerers** and **Fighters / Champion (levels 1–20)**, built with Swing and SQLite.

- **Characters:** create, open, rename, copy, search, campaigns, archive/restore, delete and JSON import/export.
- **Dashboard:** casting, class resources/actions, dice, equipment, Metamagic, spells, feats, backgrounds and character-choice reminders.
- Reversible feat benefits, protected saves, dark theme and adjustable text size.

## Run in VS Code

1. Install **JDK 25** and the **Extension Pack for Java**.
2. Open this folder and let Maven finish importing.
3. Open `src/main/java/dndcompanion/Main.java` and click **Run**, or use **F5 → DnD Companion App**.

Main class: `dndcompanion.Main`. SQLite needs `--enable-native-access=ALL-UNNAMED` (included in the launch configuration).

## Verify / run from PowerShell

With JDK 25 on PATH or in `JAVA_HOME`:

```powershell
.\scripts\Verify.ps1
java --enable-native-access=ALL-UNNAMED -cp 'target/verification/classes;target/dependency/*' dndcompanion.Main
```

Verification resolves pinned SQLite/Jackson dependencies and uses disposable databases. Logs/previews stay under `target/`. Pass `-JavaHome 'C:\path\to\jdk-25'` to choose a JDK, or `-Headless` to explicitly skip desktop checks.

## Saves

The app opens on **Characters**. Independent saves live in `data/characters.db`; an existing `data/character.db` is imported once and left untouched. Unlocked characters save when switching, exiting or committing. **Lock saved character** protects a checkpoint; locked experiments are discarded on exit or **Back to characters**. **Restore saved** returns to that checkpoint.

Copies and JSON imports receive new identities and start unlocked/unarchived. Theme and text size are global preferences. JSON files use this app's format; D&D Beyond sync, accounts, other Fighter subclasses/classes and multiclass gameplay remain deferred. Combat triggers remain player-confirmed.

## Project details

`src/main/` contains code/resources; `src/test/` contains checks; `target/` is generated output.

[Architecture](ARCHITECTURE.md) · [Character choices](CHARACTER_CHOICES.md) · [Feat automation](FEAT_AUTOMATION.md) · [Spell audit](SPELL_RULES_AUDIT.md) · [Data licensing](SPELL_DATA_LICENSE.md)
