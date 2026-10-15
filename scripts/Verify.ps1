[CmdletBinding()]
param(
    [string]$JavaHome,
    [switch]$Headless
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
    if ($JavaHome) {
        $java = Join-Path $JavaHome 'bin/java.exe'
        $javac = Join-Path $JavaHome 'bin/javac.exe'
    } else {
        $java = (Get-Command java -ErrorAction SilentlyContinue).Source
        $javac = (Get-Command javac -ErrorAction SilentlyContinue).Source
        if (-not $java -or -not $javac) {
            throw 'JDK 25 is missing from PATH. Pass -JavaHome or set JAVA_HOME to a complete JDK 25.'
        }
    }
    if (-not (Test-Path $java) -or -not (Test-Path $javac)) {
        throw 'A complete JDK 25 is required. Pass -JavaHome or set JAVA_HOME.'
    }
    $compilerVersion = & $javac -version 2>&1
    if ($LASTEXITCODE -ne 0 -or "$compilerVersion" -notmatch '^javac 25(?:\.|$)') {
        throw "JDK 25 is required; found $compilerVersion."
    }
    $ErrorActionPreference = 'Continue'
    $runtimeVersion = (& $java -version 2>&1 | Out-String)
    $ErrorActionPreference = 'Stop'
    if ($LASTEXITCODE -ne 0 -or $runtimeVersion -notmatch 'version "25(?:\.|\")') {
        throw 'The Java runtime must also be version 25.'
    }
    $output = Join-Path $projectRoot 'target/verification'
    $classes = Join-Path $output 'classes'
    $resolvedClasses = [System.IO.Path]::GetFullPath($classes)
    $targetRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'target')) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $resolvedClasses.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing to clean a classes directory outside the project target directory.'
    }
    if (Test-Path $resolvedClasses) { Remove-Item -LiteralPath $resolvedClasses -Recurse -Force }
    New-Item -ItemType Directory -Force $classes | Out-Null
    [xml]$pom = Get-Content pom.xml
    foreach ($runtime in $pom.project.dependencies.dependency) {
        if ($runtime.scope -in @('test','provided')) { continue }
        $version = [string]$runtime.version
        $artifact = [string]$runtime.artifactId
        $groupPath = ([string]$runtime.groupId).Replace('.', '/')
        $dependency = Join-Path $projectRoot "target/dependency/$artifact-$version.jar"
        if (Test-Path $dependency) { continue }
        New-Item -ItemType Directory -Force (Split-Path $dependency) | Out-Null
        $cached = Join-Path $env:USERPROFILE ".m2/repository/$groupPath/$artifact/$version/$artifact-$version.jar"
        if (Test-Path $cached) { Copy-Item $cached $dependency }
        else {
            Write-Host "Downloading $artifact $version..."
            Invoke-WebRequest "https://repo.maven.apache.org/maven2/$groupPath/$artifact/$version/$artifact-$version.jar" -OutFile "$dependency.download"
            Move-Item -LiteralPath "$dependency.download" -Destination $dependency
        }
    }
    $runtimeClasspath = ((@($pom.project.dependencies.dependency | Where-Object { $_.scope -notin @('test','provided') } | ForEach-Object { Join-Path $projectRoot "target/dependency/$($_.artifactId)-$($_.version).jar" })) -join ';')
    $sources = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object FullName)
    Write-Host "Compiling $($sources.Count) sources with $compilerVersion..."
    & $javac -encoding UTF-8 -cp $runtimeClasspath -d $classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    Copy-Item src/main/resources/* $classes -Recurse -Force
    $headlessChecks = @('SorcererChecks','PlayChecks','SpellBookChecks','SpeciesChecks','GrimoireChecks','DamageDiceChecks','SpellRulesChecks','CharacterStoreChecks','SpellRepositoryChecks','CleanupRulesChecks','FeatEffectsChecks','FeatRemovalChecks','FighterChecks','RosterStoreChecks')
    $failed = @()
    $passed = 0
    $skipped = 0
    foreach ($source in (Get-ChildItem src/test/java -Recurse -Filter *Checks.java | Sort-Object FullName)) {
        $content = Get-Content $source.FullName -Raw
        if ($content -notmatch 'public static void main\(') { continue }
        $package = [regex]::Match($content, 'package\s+([\w.]+);').Groups[1].Value
        $check = "$package.$($source.BaseName)"
        if ($Headless -and $source.BaseName -notin $headlessChecks) {
            Write-Host "SKIP $check (requires desktop verification)"
            $skipped++
            continue
        }
        Write-Host "RUN  $check"
        $ErrorActionPreference = 'Continue'
        & $java --enable-native-access=ALL-UNNAMED "-Djava.awt.headless=$($Headless.IsPresent.ToString().ToLowerInvariant())" -cp "$classes;$runtimeClasspath" $check 2>&1 | Tee-Object -FilePath (Join-Path $output "$($source.BaseName).log")
        $ErrorActionPreference = 'Stop'
        if ($LASTEXITCODE -eq 0) { Write-Host "PASS $check"; $passed++ }
        else { Write-Host "FAIL $check"; $failed += $check }
    }
    Write-Host "Verification: $passed passed, $($failed.Count) failed, $skipped skipped. Logs: $output"
    if ($failed.Count) { throw "Failed checks: $($failed -join ', ')" }
} catch {
    Write-Host "Verification failed: $_" -ForegroundColor Red
    exit 1
} finally {
    Pop-Location
}
