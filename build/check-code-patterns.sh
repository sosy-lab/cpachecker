#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail
IFS=$'\n\t'
shopt -s globstar

DIR="$(dirname "$0")/.."
ERROR=0

if ( command -v git && git rev-parse --is-inside-work-tree ) > /dev/null 2>&1; then
  # We use git ls-files instead of recursive grep because many people
  # have lots of other files inside their CPAchecker directory,
  # and scanning these could take a long time.
  FORBIDDEN_CPACHECKER_SPELLING='CPA( c| C|C)hecker'
  if [ ! -z "$(git ls-files -z "$DIR" | xargs -0 grep -I -P "$FORBIDDEN_CPACHECKER_SPELLING")" ]; then
    cat >&2 <<'EOF'
The only correct spelling of "CPAchecker" is exactly like this,
i.e., with a lowercase second "c" and no whitespace in the middle.
Please correct the following occurrences:

EOF
    git ls-files -z "$DIR" | xargs -0 grep -I -P --color=always "$FORBIDDEN_CPACHECKER_SPELLING" || true
    echo
    ERROR=1
  fi
fi


FORBIDDEN_OUTPUT='System\.(out|err)\.print'

if grep -q -P "$FORBIDDEN_OUTPUT" "$DIR/src"/**/*.java; then
  cat >&2 <<'EOF'
Use of System.out or System.err found. We do not want raw output,
please use proper logging as described in doc/Logging.md
(https://gitlab.com/sosy-lab/software/cpachecker/-/blob/trunk/doc/Logging.md).
Please fix the following cases:

EOF
  grep --color=always -P "$FORBIDDEN_OUTPUT" "$DIR/src"/**/*.java
  echo
  ERROR=1
fi


# This checks for "equals()" implementations where the body is not one of the following:
# - a single "return" statement", or
# - a "if (foo != null || getClass() != foo.getClass())" check followed by a variable declaration with a cast and a single "return" statement, or
# - a "if (!super.equals(foo) || getClass() != foo.getClass())" check followed by a variable declaration with a cast and a single "return" statement, or
# - a single "if (foo instanceof ...)" branch with a comment inside, or
# - anything preceded by a comment, or
# - anything of the above preceded by a "this == null" check, or
# - a single "throw new UnsupportedOperationException".
# 
# (?! ... ) is a negative look-ahead assertion.
# (?: ... ) is a non-capturing group.
FORBIDDEN_EQUALS_METHOD='\n( *)public (?:final )?boolean equals\((?:final )?(?:@Nullable )?Object ([^() ;]*)\) {\n(?!(?:\n* *if \(this == \2\) {\n *return true;\n *})?(?:\n* *if \((?:\2 == null|\!super\.equals\(\2\)) \|\| getClass\(\) \!= \2\.getClass\(\)\) {\n *return false;\n *}\n* *([^(){};=]*) ([^ (){};]*) = \(\3\) \2;)?\n* *(?:\/\/|return|if \(\2 instanceof [^ (){};]* [^ (){};]*(?:\n* *\&\& [^{};]*)?\) {\n* *\/\/) |\n* *throw new UnsupportedOperationException)(?:.|\n)*?\n\1}'

# With "grep -z" we can use regexp to match across lines,
# because it changes the line separator of grep to \0.
# We require -P because the regexp uses negative look-ahead assertions.

if grep -q -z -P "$FORBIDDEN_EQUALS_METHOD" "$DIR/src"/**/*.java; then
  cat >&2 <<'EOF'
Implementation of equals() found that does not conform to one of our standard patterns.
Please either use a record, or use our standard pattern for equals(),
or add a comment that indicates why the complex form is required in your case.
More information is in doc/StyleGuide.md
(https://gitlab.com/sosy-lab/software/cpachecker/-/blob/trunk/doc/StyleGuide.md#equals-methods).
These equals() implementations should be treated in this way:

EOF
  grep -o -z --color=always -P "$FORBIDDEN_EQUALS_METHOD" "$DIR/src"/**/*.java | tr '\0' '\n'
  echo
  ERROR=1
fi

[ "$ERROR" -eq 0 ] || exit 1
