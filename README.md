# DnD Companion App

An offline Java 25 desktop companion for a single PHB 2024 Sorcerer. Built with Swing and SQLite.

## Features

- **Cast & Combat:** spell casting, resources, dice rolls, equipment and feat actions.
- **Spell Library:** search, filters, spell learning and full descriptions.
- **Metamagic:** select options and view spell compatibility.
- **Feats:** PHB feats and backgrounds, automatic bonuses and spell grants.
- **Character:** abilities, skills, HP, equipment and reminders for required choices.

Dark theme and adjustable text sizes are available. Turns, enemy/ally effects and table decisions remain player-managed.

## Run in VS Code

1. Install **JDK 25** and the **Extension Pack for Java**.
2. Open the project folder and let VS Code finish importing the Maven project.
3. Open `src/main/java/dndcompanion/Main.java` and click **Run**, or press **F5** using the **DnD Companion App** launch configuration.

The main class is `dndcompanion.Main`. The launch configuration uses `--enable-native-access=ALL-UNNAMED` for SQLite.

## Verify and run from PowerShell

With JDK 25 on PATH or in `JAVA_HOME`:

```powershell
.\scripts\Verify.ps1
```

To select a JDK explicitly:

```powershell
.\scripts\Verify.ps1 -JavaHome 'C:\path\to\jdk-25'
```

Verification compiles the project, resolves SQLite JDBC and runs the checks using disposable databases. Logs and previews go to `target/verification/`. Add `-Headless` to skip desktop checks.

After verification, launch the compiled app:

```powershell
java --enable-native-access=ALL-UNNAMED -cp 'target/verification/classes;target/dependency/*' dndcompanion.Main
```

## Saving

Characters save to `data/character.db` when you close the app or use **Commit to grimoire**. **Lock saved character** protects a checkpoint; closing while locked discards experiments. **Restore saved** returns to that checkpoint.

The bundled spell database is separate from your character save. No database server is needed.

## Project files and details

- `src/main/`: application code and bundled resources.
- `src/test/`: regression checks.
- `target/`: generated classes, dependencies, logs and previews.
- [Architecture](ARCHITECTURE.md)
- [Character choices](CHARACTER_CHOICES.md)
- [Feat automation and limits](FEAT_AUTOMATION.md)
- [Spell rules audit](SPELL_RULES_AUDIT.md) and [data licensing](SPELL_DATA_LICENSE.md)
