#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk-21.0.13+9}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

cd "$(dirname "$0")/.."
./gradlew :app:assembleDebug "$@"
