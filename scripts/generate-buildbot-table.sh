#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail
IFS=$'\n\t'

BASE_URL=https://buildbot.sosy-lab.org
declare -a OPTIONS
declare -a URLS

while [[ $# -gt 0 ]]; do

  if [[ $1 == -* ]]; then
    OPTIONS+=("$1")

  elif [[ ! -v BUILDER ]]; then
    # first non-option parameter is builder id
    BUILDER="$1"

  else
    # other non-option ids are job ids
    # extract URL of results from BuildBot metadata
    URL="$(curl --silent --show-error "$BASE_URL/cpachecker/api/v2/builders/$BUILDER/builds/$1/steps?field=urls" | jq '.steps[-1].urls[] | select(.name=="TABLE (current results only)") | .url' -r)"
    URLS+=("$BASE_URL${URL%.html}.xml.bz2")
  fi

  shift
done

if [[ ! -v URLS ]]; then
  echo "Generate a regression table from BuildBot results." >&2
  echo >&2
  echo "usage: $0 BUILDER_ID JOB_ID JOB_ID [JOB_ID ...] [--table-generator-options ...]" >&2
  echo >&2
  echo "Example:" >&2
  echo "$0 2 100 200" >&2
  echo "This will generate a table comparing build 100 against build 200 of BuildBot builder 2 (smg)." >&2
  echo "If there are parameters with a dash, they will be passed through to table-generator," >&2
  echo "otherwise default parameters for showing an HTML regression table will be passed." >&2
  exit 1
fi

if [[ ! -v OPTIONS ]]; then
  # no options passed, use default
  OPTIONS=("--format=html" "--dump" "--show")
fi

echo table-generator "${OPTIONS[@]}" "${URLS[@]}"
exec table-generator "${OPTIONS[@]}" "${URLS[@]}"
