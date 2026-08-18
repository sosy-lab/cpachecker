#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""
Check consistency of CPAchecker task-definition YAML files.

This script intentionally performs only cheap static checks. It does not compile
or execute test programs. Keep all repository-specific policy below in one place
so that future extensions are easy to review and adapt.
"""

import argparse
import glob
from pathlib import Path
import sys
from typing import NamedTuple, Optional, Set


SCRIPT_PATH = Path(__file__).resolve()
EXPECTED_PARENT_DIRECTORY = Path("test") / "util"
if Path(*SCRIPT_PATH.parent.parts[-2:]) != EXPECTED_PARENT_DIRECTORY:
    raise RuntimeError(
        "expected script to be located in '{}', but found '{}'".format(
            EXPECTED_PARENT_DIRECTORY, SCRIPT_PATH.parent
        )
    )

CPACHECKER_DIR = SCRIPT_PATH.parents[2]
for wheel in glob.glob(str(CPACHECKER_DIR / "lib" / "python-benchmark" / "*.whl")):
    sys.path.insert(0, wheel)

import yaml


LANGUAGE_FILE_ENDINGS = {
    "C": (".c", ".i"),
    "Java": (".java",),
    "SV-LIB": (".svlib",),
}
LANGUAGES_ALLOWING_DIRECTORY_INPUTS = {"Java"}

SUPPORTED_DATA_MODELS = {
    "C": {"ILP32", "LP64"},
}

ADDITIONAL_FILE_KEYS = frozenset({"required_files"})


class ValidationError(NamedTuple):
    """A validation failure with its location and optional relevant text."""

    error_type: str
    location: Path
    snippet: Optional[str] = None


errors: Set[ValidationError] = set()


def report_error(path, error_type, snippet=None):
    """Add one validation error to the global error set.

    ``path`` identifies the file where validation failed, ``error_type``
    describes what went wrong, and ``snippet`` may contain the relevant source
    text. All arguments must be hashable so the resulting error can be stored in
    ``errors``.
    """
    errors.add(ValidationError(error_type, path, snippet))


def normalize_to_list(value):
    """Return a list representation of an optional scalar-or-list YAML value.

    ``None`` represents an absent value and becomes an empty list. Existing
    lists are returned unchanged; every other value becomes a one-element list.
    """
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]


def check_referenced_file_existence(path, base_dir, reference_key, references):
    """Check that file references from a YAML key point to existing files.

    ``path`` identifies the task definition for diagnostics, ``base_dir`` is
    the directory against which references are resolved, and ``reference_key``
    names the YAML key being checked. ``references`` may be ``None``, one value,
    or a list. Validation failures are added to the global ``errors`` set.
    """
    for reference in normalize_to_list(references):
        if not isinstance(reference, str):
            report_error(
                path,
                "{} contains non-string entry {!r}".format(reference_key, reference),
            )
            continue

        resolved = base_dir / reference
        if not resolved.exists():
            report_error(
                path,
                "{} references missing file '{}'".format(reference_key, reference),
            )


def check_language(path, input_files, language):
    """Check that a task language is supported and matches all input files.

    ``path`` must be the task-definition path and is also used to resolve input
    paths. ``input_files`` must be an iterable, normally normalized with
    :func:`normalize_to_list`. ``language`` may be absent; otherwise it is
    expected to name an entry in ``LANGUAGE_FILE_ENDINGS``. Errors are added to
    the global ``errors`` set.
    """
    if language and language not in LANGUAGE_FILE_ENDINGS:
        report_error(
            path,
            "unsupported programming language '{}'".format(language),
        )
        return
    if not language:
        return

    allowed_endings = LANGUAGE_FILE_ENDINGS[language]
    for input_file in input_files:
        if not isinstance(input_file, str):
            continue
        input_path = path.parent / input_file
        if input_path.is_dir() and language in LANGUAGES_ALLOWING_DIRECTORY_INPUTS:
            continue
        if not input_file.endswith(allowed_endings):
            report_error(
                path,
                "input file '{}' does not match language '{}' with endings {}".format(
                    input_file, language, ", ".join(allowed_endings)
                ),
            )


def check_data_model(path, language, data_model):
    """Check that an optional data model is supported for the task language.

    ``path`` identifies the task definition for diagnostics. ``language`` and
    ``data_model`` are the values from the task options and may be absent.
    Supported language/model combinations come from ``SUPPORTED_DATA_MODELS``;
    validation failures are added to the global ``errors`` set.
    """
    if not data_model:
        return

    supported_models = SUPPORTED_DATA_MODELS.get(language)
    if supported_models is None:
        report_error(
            path,
            "data_model is specified for language '{}' without configured models".format(
                language
            ),
        )
    elif data_model not in supported_models:
        report_error(
            path,
            "unsupported data_model '{}' for language '{}'".format(
                data_model, language
            ),
        )


def check_properties(path, content):
    """Validate the structure and referenced files of task properties.

    ``path`` must be the task-definition path and is used as the base for
    relative property-file references. ``content`` must be the parsed task
    mapping. An absent ``properties`` key is accepted, while a present value
    must be a non-empty list of mappings with a string ``property_file``.
    Validation failures are added to the global ``errors`` set.
    """
    properties = content.get("properties")
    if properties is None:
        return
    if not properties:
        report_error(path, "empty properties")
        return
    if not isinstance(properties, list):
        report_error(path, "properties is not a list")
        return

    for property_definition in properties:
        if not isinstance(property_definition, dict):
            report_error(
                path,
                "invalid property definition {!r}".format(property_definition),
            )
            continue
        property_file = property_definition.get("property_file")
        if property_file is None or property_file == "":
            report_error(path, "property definition without property_file")
            continue
        if not isinstance(property_file, str):
            report_error(
                path,
                "property_file contains non-string entry {!r}".format(property_file),
            )
            continue
        resolved = path.parent / property_file
        if not resolved.exists():
            report_error(
                path,
                "property_file references missing file '{}'".format(property_file),
            )


def check_task_definition(path, content):
    """Run all structural and semantic checks on one parsed task definition.

    ``path`` must be the path of the YAML file. ``content`` is the value
    returned by the YAML parser and is validated before it is treated as a
    mapping. Validation failures are added to the global ``errors`` set.
    """
    if not isinstance(content, dict):
        report_error(path, "expected mapping for task definition")
        return

    input_files = normalize_to_list(content.get("input_files"))
    if not input_files:
        report_error(path, "missing or empty input_files")
    check_referenced_file_existence(path, path.parent, "input_files", input_files)

    for additional_file_key in ADDITIONAL_FILE_KEYS:
        check_referenced_file_existence(
            path,
            path.parent,
            additional_file_key,
            content.get(additional_file_key),
        )

    options = content.get("options")
    if options is None:
        options = {}
    if not isinstance(options, dict):
        report_error(path, "options is not a mapping")
        options = {}

    language = options.get("language")
    check_language(path, input_files, language)
    check_data_model(path, language, options.get("data_model"))
    check_properties(path, content)


def check_yaml_file(path):
    """Load and validate one task-definition YAML file.

    ``path`` must refer to a readable YAML file. YAML syntax and task-validation
    errors are added to the global ``errors`` set.
    """
    try:
        with path.open(encoding="utf-8") as yml_file:
            content = yaml.safe_load(yml_file)
    except yaml.YAMLError as exception:
        report_error(path, "invalid YAML: {}".format(exception))
        return

    check_task_definition(path, content)


def read_set_file(path):
    """Read a task-set file and return its lines without newline characters.

    ``path`` must refer to the set file to read. Read failures are reported via
    the global ``errors`` set and result in an empty list.
    """
    try:
        return path.read_text(encoding="utf-8").splitlines()
    except OSError as exception:
        report_error(path, "could not read set file: {}".format(exception))
        return []


def task_definition_files_from_set(path):
    """Yield task-definition files selected by the entries of a set file.

    ``path`` must refer to a task-set file. Non-empty, non-comment lines are
    interpreted as glob patterns relative to its parent directory. Unmatched
    patterns and read failures are added to ``errors``; matched files
    without the ``.yml`` suffix are ignored.
    """
    for line in read_set_file(path):
        line = line.strip()
        if not line or line.startswith("#"):
            continue

        matches = sorted(path.parent.glob(line))
        if not matches:
            report_error(
                path,
                "set entry '{}' does not match any file".format(line),
            )
            continue

        for match in matches:
            if match.suffix == ".yml":
                yield match


def task_definition_files(root):
    """Yield task-definition files below or represented by ``root``.

    ``root`` must exist and be a directory, a ``.yml`` task definition, or a
    ``.set`` task set. The caller checks existence first. Unsupported file types
    and invalid set entries are added to the global ``errors`` set.
    """
    if root.is_file():
        if root.suffix == ".yml":
            yield root
        elif root.suffix == ".set":
            yield from task_definition_files_from_set(root)
        else:
            report_error(root, "not a task-definition .yml or set file")
        return

    for path in root.rglob("*.yml"):
        yield path


def parse_args():
    """Parse command-line roots and return the populated argument namespace."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "roots",
        nargs="+",
        type=Path,
        help=(
            "Directories, task-definition .yml files, or .set files. "
            "Relative paths are resolved from the current working directory."
        ),
    )
    return parser.parse_args()


def main():
    """Validate all task definitions selected on the command line.

    Return zero if no error was found and one otherwise.
    """
    args = parse_args()
    errors.clear()

    for root in args.roots:
        if not root.exists():
            report_error(root, "does not exist")
            continue
        for task_definition in task_definition_files(root):
            check_yaml_file(task_definition)

    for error in sorted(
        errors,
        key=lambda item: (str(item.location), item.error_type, item.snippet or ""),
    ):
        print("ERROR: {}: {}".format(error.location, error.error_type))
        if error.snippet is not None:
            print("  {}".format(error.snippet))

    if not errors:
        print("Task-definition validation successful.")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
