#!/usr/bin/env bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail
IFS=$'\n\t'

VERSION="${1:-}"
if [[ -z $VERSION ]]; then
  echo "Please provide the version of CPAchecker as first parameter."
  exit 1
fi
CPACHECKER="CPAchecker-$VERSION-unix"

ARCHIVE="${2:-}"
if [[ -z $ARCHIVE ]]; then
  echo "Please provide the CPAchecker-$VERSION-unix.zip file as second parameter."
  exit 1
fi
if [[ $(basename "$ARCHIVE") != $CPACHECKER.zip ]]; then
  echo "The given archive '$ARCHIVE' does not seem to be CPAchecker for version '$VERSION' on Linux."
  exit 1
fi
if [[ ! -f $ARCHIVE ]]; then
  echo "Archive '$ARCHIVE' does not exist."
  exit 1
fi
TARGET="${3:-}"
if [[ -z $TARGET ]]; then
  echo "Please provide target directory as third parameter."
  exit 1
fi
mkdir -p "$TARGET"

export CPACHECKER VERSION

TEMP_DEB="$(mktemp -d)"
BUILD_DIR="$TEMP_DEB/build"
mkdir "$BUILD_DIR"
cp -a "$(dirname "$0")/debian" "$BUILD_DIR"

podman run --rm -w "$BUILD_DIR" -v "$TEMP_DEB:$TEMP_DEB:rw" -v "$(realpath "$ARCHIVE"):/$CPACHECKER.zip:ro" -e CPACHECKER -e "DEB*" -e VERSION ubuntu:20.04 bash -c '
  set -euo pipefail

  apt-get update
  apt-get install -y --no-install-recommends dpkg-dev devscripts unzip
  TZ=UTC DEBIAN_FRONTEND=noninteractive apt-get install -y $(dpkg-checkbuilddeps 2>&1 | grep -o "Unmet build dependencies:.*" | cut -d: -f2- | sed "s/([^)]*)//g")

  unzip -q "/$CPACHECKER.zip"
  # clean up irrelevant stuff
  rm -rf "$CPACHECKER/lib/native/"{x86-*,*macosx,*windows}

  dch -v "$VERSION-1" "New upstream version."
  dch -r ""

  dpkg-buildpackage --build=source,binary --no-sign
'

cp -a "$BUILD_DIR"/debian/changelog "$(dirname "$0")/debian"
cp -a "$TEMP_DEB"/*.* "$TARGET/"
rm -rf "$TEMP_DEB"
