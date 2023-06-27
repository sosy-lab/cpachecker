#!/bin/bash
set -euo pipefail
IFS=$'\n\t'
shopt -s globstar

# This checks for "equals()" implementations where the body is not one of the following:
# - a single "return" statement", or
# - a "if (foo != null || getClass() != foo.getClass())" check followed by a variable declaration with a cast and a single "return" statement, or
# - a "if (!super.equals(foo) || getClass() != foo.getClass())" check followed by a variable declaration with a cast and a single "return" statement, or
# - a single "if (foo instanceof ...)" branch with a comment inside, or
# - anything preceded by a comment, or
# - anything of the above preceded by a "this == null" check, or
# - a single "throw new UnsupportedOperationException".
# 
FORBIDDEN_EQUALS_METHOD='\n( *)public boolean equals\((@Nullable )?Object ([^() ;]*)\) {\n(?!(\n* *if \(this == \3\) {\n *return true;\n *})?(\n* *if \((\3 == null|\!super\.equals\(\3\)) \|\| getClass\(\) \!= \3\.getClass\(\)\) {\n *return false;\n *}\n* *([^(){};=]*) ([^ (){};]*) = \(\7\) \3;)?\n* *(\/\/|return|if \(\3 instanceof [^ (){};]* [^ (){};]*(\n* *\&\& [^{};]*)?\) {\n* *\/\/) |\n* *throw new UnsupportedOperationException)(.|\n)*?\n\1}'

# With "grep -z" we can use regexp to match across lines,
# because it changes the line separator of grep to \0.
# We require -P because the regexp uses negative look-ahead assertions.

if grep -q -z -P "$FORBIDDEN_EQUALS_METHOD" src/**/*.java; then
  cat >&2 <<'EOF'
Implementation of equals() found that does not conform to one of our standard patterns.
Please either use a record, or use our standard pattern for equals(),
or add a comment that indicates why the complex form is required in your case.
More information is in doc/StyleGuide.md
(https://gitlab.com/sosy-lab/software/cpachecker/-/blob/trunk/doc/StyleGuide.md#equals-methods).
These equals() implementations should be treated in this way:

EOF
  grep -o -z --color=always -P "$FORBIDDEN_EQUALS_METHOD" src/**/*.java | tr '\0' '\n'
  exit 1
fi
