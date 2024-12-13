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

# Dependencies that should be considered for update in form "ORG:MODULE"
DEPENDENCIES=(
    # Guava is a high-quality library with a no-incompatible-changes policy
    "com.google.guava:guava"
    # only used for testing
    "com.google.guava:guava-testlib"
    "com.google.truth:truth"
    "com.google.truth.extensions:truth-java8-extension"
    "org.mockito:mockito-core"
    "org.mockito:mockito-errorprone"
    "junit:junit"
    # Build tools
    "com.google.errorprone:error_prone_core"
    "com.google.errorprone:error_prone_annotations"
    "org.eclipse.jdt.core.compiler:ecj"
    "com.github.sevntu-checkstyle:sevntu-checks"
    "com.puppycrawl.tools:checkstyle"
    "com.github.spotbugs:spotbugs-ant"
    "com.github.spotbugs:spotbugs-annotations"
    "de.thetaphi:forbiddenapis"
)
: "${BRANCH:=auto-update-dependencies}"

# Configure git if running in CI, either from GitLab or manually set variables
[[ -v GITLAB_USER_NAME ]] && git config --global user.name "${GITLAB_USER_NAME}"
[[ -v GITLAB_USER_EMAIL ]] && git config --global user.email "${GITLAB_USER_EMAIL}"
[[ -v GIT_NAME ]] && git config --global user.name "${GIT_NAME}"
[[ -v GIT_EMAIL ]] && git config --global user.email "${GIT_EMAIL}"

# If unmerged changes from a previous run of this script exist,
# we want to get into a state where we have all changes from main the auto-update branch.
# But this is only relevant if we are going to push in the end.
# If a developer executes the script locally, we just run on the current branch.
if [[ -v PUSH ]] && git fetch "$PUSH" "$BRANCH" "${CI_COMMIT_BRANCH:-main}" 2>/dev/null; then
  git checkout "$BRANCH"
  git merge --no-edit "${CI_COMMIT_BRANCH:-main}"
fi

previous_commit="$(git rev-parse HEAD)"

# Update google-java-format and apply formatting
ant update-lib -Dupdate-lib.org=com.google.googlejavaformat -Dupdate-lib.module=google-java-format \
    format-source
if [[ ! -z "$(git status --porcelain --untracked-files=no src)" ]]; then
  # If it reformatted something, we want to create a separate commit.
  git commit . --message "Auto-update google-java-format and reformat"
fi
# If google-java-format is updated but did not reformat anything, it can be part of the following commit.

# Update other dependencies from DEPENDENCIES
for DEP in "${DEPENDENCIES[@]}"; do
  IFS=":" read -r -a DEP <<< "$DEP"
  ant update-lib "-Dupdate-lib.org=${DEP[0]}" "-Dupdate-lib.module=${DEP[1]}"
done

# Copy version of Error Prone to override declaration and .gitlab-ci.yml
error_prone_version="$(xmlstarlet sel -t -v '/ivy-module/dependencies/dependency[@org="com.google.errorprone" and @name="error_prone_core"]/@rev' lib/ivy.xml)"
# We use sed instead of xmlstarlet/yq to preserve formatting.
sed -e '/^ *<override org="com.google.errorprone" module="error_prone_annotations" rev="[^"]*" *\/>$/ s/rev="[^"]*"/rev="'"${error_prone_version}"'"/' -i lib/ivy.xml
sed -e "s/^\( *REFASTER_VERSION: *\).*/\1${error_prone_version}/" -i .gitlab-ci.yml


# Now commit and push if necessary
if [[ ! -z "$(git status --porcelain --untracked-files=no)" ]]; then
  git commit . --message "Auto-update dependencies"
fi

current_commit="$(git rev-parse HEAD)"

if [[ "$current_commit" != "$previous_commit" ]]; then
  if [[ -v PUSH ]]; then
    # Create or update MR
    git push "$PUSH" "HEAD:refs/heads/$BRANCH" \
        -o merge_request.create \
        -o merge_request.merge_when_pipeline_succeeds \
        -o merge_request.title="Auto-update dependencies $(date -I)"
  else
    echo
    echo "==============================================="
    echo "Dependencies updated and committed."
    echo "Omitting push because variable PUSH is not set."
    echo "The changes are:"
    echo
    git --no-pager show "$previous_commit..$current_commit"
  fi
fi
