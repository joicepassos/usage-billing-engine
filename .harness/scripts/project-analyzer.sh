#!/usr/bin/env bash
# Read-only summary of a Gradle/Java project. Changes nothing.
set -euo pipefail
root="${1:-.}"
cd "$root"

echo "root: $(pwd)"
echo "files: $(ls -1 | head -50 | tr '\n' ' ')"

if [[ -x ./gradlew ]]; then echo "gradle wrapper: present"; else echo "gradle wrapper: missing"; fi

if [[ -f settings.gradle.kts ]]; then
  echo "root project: $(grep -Eo 'rootProject.name *= *"[^"]+"' settings.gradle.kts | sed -E 's/.*"(.*)"/\1/' || true)"
  includes=$(grep -Eo 'include\([^)]*\)' settings.gradle.kts | tr '\n' ' ' || true)
  echo "subprojects: ${includes:-none}"
fi

if [[ -f build.gradle.kts ]]; then
  java=$(grep -Eo 'JavaLanguageVersion.of\([0-9]+\)' build.gradle.kts | grep -Eo '[0-9]+' || true)
  echo "java toolchain: ${java:-not declared in root build}"
fi

if [[ -f gradle/libs.versions.toml ]]; then
  echo "version catalog: gradle/libs.versions.toml"
  sed -n '/^\[versions\]/,/^\[/p' gradle/libs.versions.toml | grep -E '^[a-zA-Z]' | sed 's/^/  /' || true
fi

src=$(find . -path ./build -prune -o -type d -path '*/src/main/java' -print 2>/dev/null | head -5)
if [[ -n "$src" ]]; then
  echo "source roots:"; echo "$src" | sed 's/^/  /'
  app=$(grep -rl '@SpringBootApplication' --include='*.java' . 2>/dev/null | head -1 || true)
  if [[ -n "$app" ]]; then
    base=$(dirname "$app")
    echo "application: $app"
    echo "modules (top-level packages next to the application class):"
    find "$base" -mindepth 1 -maxdepth 1 -type d -printf '  %f\n' 2>/dev/null || ls -1d "$base"/*/ 2>/dev/null | xargs -n1 basename | sed 's/^/  /'
  fi
fi

migrations=$(find . -path ./build -prune -o -type d -path '*db/migration' -print 2>/dev/null | head -1)
[[ -n "$migrations" ]] && echo "migrations: $migrations ($(ls -1 "$migrations" | wc -l | tr -d ' ') files)"

for f in docs/PRD.md docs/PLAN.md CONTEXT.md compose.yaml docker-compose.yml; do
  [[ -e "$f" ]] && echo "found: $f"
done
[[ -d docs/adr ]] && echo "adrs: $(ls -1 docs/adr 2>/dev/null | grep -c '\.md$' || true)"
exit 0
