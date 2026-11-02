#!/usr/bin/env bash
# Lists the Gradle quality gates; with --run, executes them.
# --run executes the project's own build scripts: use only on trusted code, with approval.
set -uo pipefail
root="${1:-.}"
run=false
for a in "$@"; do [[ "$a" == "--run" ]] && run=true; done
cd "$root" || { echo "FAILED: cannot enter $root"; exit 1; }

if [[ ! -x ./gradlew ]]; then echo "NOT_RUN: ./gradlew not found or not executable"; exit 0; fi

tasks=("check" "build")
bad=false
for t in "${tasks[@]}"; do
  if ! $run; then echo "NOT_RUN $t: available; use --run to execute"; continue; fi
  if command -v timeout >/dev/null; then cmd=(timeout 1800 ./gradlew "$t" --console=plain); else cmd=(./gradlew "$t" --console=plain); fi
  if "${cmd[@]}"; then echo "PASSED $t"; else echo "FAILED $t"; bad=true; fi
done
$bad && exit 1 || exit 0
