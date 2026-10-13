param([string]$Java = 'java', [string]$DriverPath = '', [string]$SourcePath = '')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (!$DriverPath) { $DriverPath = Join-Path $projectRoot 'target/dependency/sqlite-jdbc-3.53.4.0.jar' }
if (!$SourcePath) { $SourcePath = Join-Path $PSScriptRoot 'data/spell-catalog.tsv' }
if (!(Test-Path -LiteralPath $DriverPath)) { throw "SQLite JDBC driver not found: $DriverPath" }
& $Java --enable-native-access=ALL-UNNAMED -cp $DriverPath (Join-Path $PSScriptRoot 'BuildSpellDatabase.java') $SourcePath (Join-Path $projectRoot 'src/main/resources/spells/spells.db')
if ($LASTEXITCODE -ne 0) { throw 'Spell catalog build failed; the existing database was preserved.' }
