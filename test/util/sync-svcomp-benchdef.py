#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""Keep test/test-sets/svcomp.xml in sync with the SV-COMP benchmark definition.

The upstream benchmark definition of CPAchecker for SV-COMP lives in
https://gitlab.com/sosy-lab/sv-comp/bench-defs/-/blob/main/benchmark-defs/cpachecker.xml
and assumes the directory layout of the competition (sv-benchmarks checked out
next to the tool directory).  Our copy in test/test-sets/svcomp.xml uses the
paths of this repository instead and additionally carries a local preamble
(license header and options that are only useful for local testing).

This script applies exactly those adaptations to the upstream file and either
compares the result with our copy (default) or writes it (--write).
"""

import argparse
import difflib
import re
import sys
import urllib.request
from pathlib import Path

UPSTREAM_PROJECT = "https://gitlab.com/sosy-lab/sv-comp/bench-defs/-"
UPSTREAM_URL = UPSTREAM_PROJECT + "/blob/main/benchmark-defs/cpachecker.xml"
DEFAULT_TEMPLATE = UPSTREAM_PROJECT + "/raw/main/benchmark-defs/cpachecker.xml"
DEFAULT_OUTPUT = Path(__file__).parent.parent / "test" / "test-sets" / "svcomp.xml"

# Paths of the competition layout mapped to the paths of this repository.
# test/programs/benchmarks is a checkout of sv-benchmarks/c,
# test/programs/svlib-benchmarks is a checkout of sv-benchmarks/sv-lib.
PATH_REPLACEMENTS = {
    "../sv-benchmarks/c/": "../programs/benchmarks/",
    "../sv-benchmarks/sv-lib/": "../programs/svlib-benchmarks/",
}

# Elements of the upstream file that make no sense for local benchmarking.
# <require> pins the runs to the competition machines,
# which are typically not the machines we benchmark on (cf. --keep-cpu-model).
DROPPED_ELEMENTS = ("require",)

# Attributes of <benchmark> that make no sense for local benchmarking.
DROPPED_BENCHMARK_ATTRIBUTES = ("displayName",)

# Everything of our copy up to and including this marker is kept as it is,
# the remainder is generated from the upstream file.
LOCAL_PREAMBLE_END = UPSTREAM_URL + "\n  -->\n"

BENCHMARK_TAG = re.compile(r"<benchmark\s[^>]*>")
ATTRIBUTE = re.compile(r'(\w+)="([^"]*)"')
EMPTY_OPTION = re.compile(r'^(\s*)<option name="([^"]*)"\s*/>\s*$')
SVCOMP_CONFIG_OPTION = re.compile(r"^--svcomp\d+$")
REQUIRE_TAG = re.compile(r"^\s*<require\b", re.MULTILINE)


def read_template(location):
    if location.startswith(("http://", "https://")):
        with urllib.request.urlopen(location) as response:
            return response.read().decode("utf-8")
    return Path(location).read_text(encoding="utf-8")


def merge_split_options(lines):
    """Merge BenchExec's split options into the form used in our test sets.

    Upstream writes an option and its value as two elements
    (<option name="--heap" /> <option name="10000M" />),
    our test sets write <option name="--heap">10000M</option>.
    """
    result = []
    pending = None  # (indentation, option name) of an option without value yet
    for line in lines:
        match = EMPTY_OPTION.match(line)
        name = match.group(2) if match else None
        is_value = name is not None and not name.startswith("-")
        if pending and is_value:
            indentation, option = pending
            result.append(f'{indentation}<option name="{option}">{name}</option>')
            pending = None
            continue
        if pending:
            indentation, option = pending
            result.append(f'{indentation}<option name="{option}"/>')
            pending = None
        if is_value:
            sys.exit(f"Value of an option without a preceding option: {line.strip()}")
        if match:
            pending = (match.group(1), name)
        else:
            result.append(line)
    if pending:
        indentation, option = pending
        result.append(f'{indentation}<option name="{option}"/>')
    return result


def drop_elements(lines, tags):
    return [
        line
        for line in lines
        if not any(line.lstrip().startswith(f"<{tag}") for tag in tags)
    ]


def replace_paths(lines):
    result = []
    for line in lines:
        for competition_path, local_path in PATH_REPLACEMENTS.items():
            line = line.replace(competition_path, local_path)
        if "sv-benchmarks" in line:
            sys.exit(f"No path replacement known for: {line.strip()}")
        result.append(line)
    return result


def set_svcomp_config(lines, config):
    """Use the given CPAchecker SV-COMP configuration instead of the upstream one."""
    result = []
    for line in lines:
        match = re.match(r'^(\s*)<option name="(--svcomp\d+)"\s*/>\s*$', line)
        if match:
            line = f'{match.group(1)}<option name="--{config}"/>'
        result.append(line)
    return result


def benchmark_tag(template):
    match = BENCHMARK_TAG.search(template)
    if not match:
        sys.exit("No <benchmark> element found in the template")
    attributes = [
        f'{name}="{value}"'
        for name, value in ATTRIBUTE.findall(match.group(0))
        if name not in DROPPED_BENCHMARK_ATTRIBUTES
    ]
    return "<benchmark " + " ".join(attributes) + ">"


def local_preamble(current):
    """Return the local part of our copy (license header, DOCTYPE, local options)."""
    end = current.find(LOCAL_PREAMBLE_END)
    if end < 0:
        sys.exit(
            f"Cannot find the end of the local preamble in the current file.\n"
            f"It is expected to end with the comment that points to\n{LOCAL_PREAMBLE_END}"
        )
    preamble = current[: end + len(LOCAL_PREAMBLE_END)]
    if not BENCHMARK_TAG.search(preamble):
        sys.exit("The local preamble does not contain the <benchmark> element")
    return preamble


def current_keeps_cpu_model(current):
    return REQUIRE_TAG.search(current) is not None


def current_svcomp_config(current):
    configs = [
        name
        for name in re.findall(r'<option name="(--svcomp\d+)"', current)
        if SVCOMP_CONFIG_OPTION.match(name)
    ]
    return configs[0].lstrip("-") if configs else None


def generate(template, current, config, keep_cpu_model):
    preamble = BENCHMARK_TAG.sub(benchmark_tag(template), local_preamble(current))

    body_start = template.index(">", BENCHMARK_TAG.search(template).start()) + 1
    body = template[body_start:]
    lines = body.split("\n")
    lines = drop_elements(lines, () if keep_cpu_model else DROPPED_ELEMENTS)
    lines = merge_split_options(lines)
    lines = replace_paths(lines)
    if config:
        lines = set_svcomp_config(lines, config)
    body = "\n".join(lines).strip("\n")

    return preamble + "\n" + body + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--template",
        default=DEFAULT_TEMPLATE,
        metavar="URL_OR_FILE",
        help="upstream benchmark definition (default: %(default)s)",
    )
    parser.add_argument(
        "--output",
        default=DEFAULT_OUTPUT,
        type=Path,
        metavar="FILE",
        help="our benchmark definition (default: %(default)s)",
    )
    parser.add_argument(
        "--write",
        action="store_true",
        help="write the generated file instead of only comparing it",
    )
    parser.add_argument(
        "--keep-cpu-model",
        action=argparse.BooleanOptionalAction,
        help="keep the <require cpuModel=.../> element of the upstream file "
        "(default: keep it iff the current file has it)",
    )
    parser.add_argument(
        "--svcomp-config",
        metavar="svcompNN",
        help="CPAchecker configuration to use "
        "(default: the one used in the current file, otherwise the upstream one)",
    )
    arguments = parser.parse_args()

    current = arguments.output.read_text(encoding="utf-8")
    config = arguments.svcomp_config or current_svcomp_config(current)
    keep_cpu_model = arguments.keep_cpu_model
    if keep_cpu_model is None:
        keep_cpu_model = current_keeps_cpu_model(current)
    generated = generate(
        read_template(arguments.template), current, config, keep_cpu_model
    )

    if generated == current:
        print(f"{arguments.output} is in sync with {arguments.template}")
        return 0

    if arguments.write:
        arguments.output.write_text(generated, encoding="utf-8")
        print(f"Updated {arguments.output}")
        return 0

    sys.stdout.writelines(
        difflib.unified_diff(
            current.splitlines(keepends=True),
            generated.splitlines(keepends=True),
            fromfile=str(arguments.output),
            tofile=f"generated from {arguments.template}",
        )
    )
    print(
        f"\n{arguments.output} differs from {arguments.template}, "
        "use --write to apply the changes above.",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
