from pathlib import Path
from subprocess import Popen

LICENSE = """
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
""".strip()

TEMPLATE = """
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <stdio.h>

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() {{ __assert_fail("0", "fscanf_{typename}_read_{qualifier}.c", 3, "reach_error"); }}

int main() {{
{type} i = 0;

fscanf(stdin,"%{qualifier}", &i);

if(i) {{
    reach_error();
}}

return 0;
}}
""".strip()

YAML = """
# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0
format_version: '2.0'
input_files: 'fscanf_{type}_read_{qualifier}.i'
properties:
  - property_file: '../../../config/properties/unreach-call.prp'
    expected_verdict: false
options:
  language: C
  data_model: LP64
""".strip()

TYPES = ["int", "short", "long", "long long"]
QUALIFIERS = [
    "d",
    "i",
    "o",
    "u",
    "x",
    "ld",
    "li",
    "lo",
    "lu",
    "lx",
    "lld",
    "lli",
    "llo",
    "llu",
    "llx",
    "hd",
    "hi",
    "ho",
    "hu",
    "hx",
]


def gen_simple():
    for type in TYPES:
        for qualifier in QUALIFIERS:
            name = Path(
                "fscanf_{type}_read_{qualifier}.c".format(
                    type=type.replace(" ", "_"), qualifier=qualifier
                )
            )
            with open(name, "w") as f:
                f.write(
                    TEMPLATE.format(
                        type=type, typename=type.replace(" ", "_"), qualifier=qualifier
                    )
                )
            with open(name.with_suffix(".yml"), "w") as f:
                f.write(YAML.format(type=type.replace(" ", "_"), qualifier=qualifier))

            Popen(["gcc", "-E", "-std=c11", name, "-o", name.with_suffix(".i")])

            with open(name.with_suffix(".i.license"), "w") as f:
                f.write(LICENSE)


if __name__ == "__main__":
    gen_simple()
