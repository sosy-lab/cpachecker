from pathlib import Path
import subprocess

LICENSE = b"""
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
void reach_error() {{ __assert_fail("0", "{name}.c", 3, "reach_error"); }}

int main() {{
{type} i = 0;

fscanf(stdin,"%{qualifier}", &i);

if(i > 0) {{
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
input_files: '{name}.i'
properties:
  - property_file: '{property}'
    expected_verdict: {verdict}
options:
  language: C
  data_model: LP64
""".strip()

QUALIFIERS = {
    "d": "int",
    "i": "int",
    "o": "unsigned int",
    "u": "unsigned int",
    "x": "unsigned int",
    "ld": "long",
    "li": "long",
    "lo": "unsigned long",
    "lu": "unsigned long",
    "lx": "unsigned long",
    "lld": "long long",
    "lli": "long long",
    "llo": "unsigned long long",
    "llu": "unsigned long long",
    "llx": "unsigned long long",
    "hd": "short",
    "hi": "short",
    "ho": "unsigned short",
    "hu": "unsigned short",
    "hx": "unsigned short",
    "hhd": "signed char",
    "hhi": "signed char",
    "hho": "unsigned char",
    "hhu": "unsigned char",
    "hhx": "unsigned char",
    "f": "float",
    "lf": "double",
    "Lf": "long double",
}


def write_c_program(name: Path, type: str, qualifier: str):
    with open(name, "w") as f:
        f.write(TEMPLATE.format(type=type, name=name.stem, qualifier=qualifier))

    output = subprocess.run(
        ["gcc", "-E", "-std=c11", name], stdout=subprocess.PIPE, check=True
    )

    # Unfortunately gcc -C does not keep the license comment at top of the
    # file, so we first write the license followed by the preprocessor output.
    with name.with_suffix(".i").open("wb") as f:
        f.write(LICENSE)
        f.write(b"\n")
        f.write(output.stdout)


def write_yml(name: Path, property: str, verdict: str):
    with open(name.with_suffix(".yml"), "w") as f:
        f.write(YAML.format(name=name.stem, property=property, verdict=verdict))


def gen_simple():
    all_types = set(QUALIFIERS.values())

    for qualifier, type in QUALIFIERS.items():
        name = Path(
            "fscanf_gen_{type}_read_{qualifier}.c".format(
                type=type.replace(" ", "_"), qualifier=qualifier
            )
        )

        write_c_program(name, type, qualifier)
        write_yml(name, "../../../../config/properties/unreach-call.prp", "false")

        bad_type = (all_types - set(type)).pop()
        name = Path(
            "fscanf_gen_undef_{type}_read_{qualifier}.c".format(
                type=bad_type.replace(" ", "_"), qualifier=qualifier
            )
        )

        write_c_program(name, bad_type, qualifier)
        write_yml(name, "../../../../config/properties/unreach-call.prp", "false")


if __name__ == "__main__":
    gen_simple()
