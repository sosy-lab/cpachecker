#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""Check consistency of CPAchecker task-definition YAML files.

This script intentionally performs only cheap static checks. It does not compile
or execute test programs. Keep all repository-specific policy below in one place
so that future extensions are easy to review and adapt.
"""

import argparse
import glob
from pathlib import Path
import sys


CPACHECKER_DIR = Path(__file__).resolve().parents[2]
for wheel in glob.glob(str(CPACHECKER_DIR / "lib" / "python-benchmark" / "*.whl")):
    sys.path.insert(0, wheel)

try:
    import yaml
except ImportError:
    yaml = None


TASK_DEFINITION_ROOTS = (Path("test/programs"),)
TASK_DEFINITION_SEARCH_ROOTS = (
    Path("test/programs"),
    Path("test/programs/simple"),
)

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


def report_error(error_count, path, message):
    print("ERROR: {}: {}".format(path, message))
    error_count[0] += 1


def normalize_to_list(value):
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]


def check_referenced_file_existence(
    path, base_dir, reference_key, references, error_count
):
    """Check that file references from a YAML key point to existing files."""
    for reference in normalize_to_list(references):
        if not isinstance(reference, str):
            report_error(
                error_count,
                path,
                "{} contains non-string entry {!r}".format(reference_key, reference),
            )
            continue

        resolved = base_dir / reference
        if not resolved.exists():
            report_error(
                error_count,
                path,
                "{} references missing file '{}'".format(reference_key, reference),
            )


def check_language(path, input_files, language, error_count):
    if language and language not in LANGUAGE_FILE_ENDINGS:
        report_error(
            error_count,
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
                error_count,
                path,
                "input file '{}' does not match language '{}' with endings {}".format(
                    input_file, language, ", ".join(allowed_endings)
                ),
            )


def check_required_language(path, language, error_count):
    if not language:
        report_error(
            error_count, path, "missing programming language in options.language"
        )
        return
    if language not in LANGUAGE_FILE_ENDINGS:
        report_error(
            error_count,
            path,
            "unsupported programming language '{}'".format(language),
        )


def check_data_model(path, language, data_model, error_count):
    if not data_model:
        return

    supported_models = SUPPORTED_DATA_MODELS.get(language)
    if supported_models is None:
        report_error(
            error_count,
            path,
            "data_model is specified for language '{}' without configured models".format(
                language
            ),
        )
    elif data_model not in supported_models:
        report_error(
            error_count,
            path,
            "unsupported data_model '{}' for language '{}'".format(
                data_model, language
            ),
        )


def check_properties(
    path, content, error_count, require_properties, check_property_files
):
    properties = content.get("properties")
    if not properties:
        if require_properties:
            report_error(error_count, path, "missing or empty properties")
        return
    if not isinstance(properties, list):
        report_error(error_count, path, "properties is not a list")
        return

    for property_definition in properties:
        if not isinstance(property_definition, dict):
            report_error(
                error_count,
                path, "invalid property definition {!r}".format(property_definition)
            )
            continue
        property_file = property_definition.get("property_file")
        if property_file is None or property_file == "":
            report_error(error_count, path, "property definition without property_file")
            continue
        if not isinstance(property_file, str):
            report_error(
                error_count,
                path,
                "property_file contains non-string entry {!r}".format(property_file),
            )
            continue
        if not check_property_files:
            continue
        resolved = path.parent / property_file
        if not resolved.exists():
            report_error(
                error_count,
                path, "property_file references missing file '{}'".format(property_file)
            )


def check_task_definition(path, content, error_count, args):
    if not isinstance(content, dict):
        report_error(error_count, path, "expected mapping for task definition")
        return

    input_files = normalize_to_list(content.get("input_files"))
    if not input_files:
        report_error(error_count, path, "missing or empty input_files")
    check_referenced_file_existence(
        path, path.parent, "input_files", input_files, error_count
    )

    for additional_file_key in ADDITIONAL_FILE_KEYS:
        check_referenced_file_existence(
            path,
            path.parent,
            additional_file_key,
            content.get(additional_file_key),
            error_count,
        )

    options = content.get("options")
    if options is None:
        options = {}
    if not isinstance(options, dict):
        report_error(error_count, path, "options is not a mapping")
        options = {}

    language = options.get("language")
    if args.require_language:
        check_required_language(path, language, error_count)
    check_language(path, input_files, language, error_count)
    if args.check_data_model:
        check_data_model(path, language, options.get("data_model"), error_count)
    check_properties(
        path, content, error_count, args.require_properties, args.check_property_files
    )


def check_yaml_file(path, error_count, args):
    try:
        with path.open(encoding="utf-8") as yml_file:
            content = yaml.safe_load(yml_file)
    except yaml.YAMLError as exception:
        report_error(error_count, path, "invalid YAML: {}".format(exception))
        return

    check_task_definition(path, content, error_count, args)


def read_set_file(path, error_count):
    try:
        return path.read_text(encoding="utf-8").splitlines()
    except OSError as exception:
        report_error(error_count, path, "could not read set file: {}".format(exception))
        return []


def task_definition_files_from_set(path, error_count):
    for line in read_set_file(path, error_count):
        line = line.strip()
        if not line or line.startswith("#"):
            continue

        matches = sorted(path.parent.glob(line))
        if not matches:
            report_error(
                error_count,
                path,
                "set entry '{}' does not match any file".format(line),
            )
            continue

        for match in matches:
            if match.suffix in (".yml", ".yaml"):
                yield match


def task_definition_files(root, error_count):
    if root.is_file():
        if root.suffix in (".yml", ".yaml"):
            yield root
        elif root.suffix == ".set":
            yield from task_definition_files_from_set(root, error_count)
        else:
            report_error(error_count, root, "not a task-definition YAML or set file")
        return

    for path in sorted(root.rglob("*.yml")):
        yield path
    for path in sorted(root.rglob("*.yaml")):
        yield path


def resolve_root(root, error_count):
    if root.is_absolute():
        return root

    candidates = [CPACHECKER_DIR / root]
    candidates.extend(
        CPACHECKER_DIR / search_root / root
        for search_root in TASK_DEFINITION_SEARCH_ROOTS
    )

    existing_candidates = [candidate for candidate in candidates if candidate.exists()]
    if len(existing_candidates) == 1:
        return existing_candidates[0]
    if len(existing_candidates) > 1:
        report_error(
            error_count,
            CPACHECKER_DIR / root,
            "ambiguous root, matches {}".format(
                ", ".join(
                    str(candidate.relative_to(CPACHECKER_DIR))
                    for candidate in existing_candidates
                )
            ),
        )
        return existing_candidates[0]

    return candidates[0]


def parse_args(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "roots",
        nargs="*",
        type=Path,
        default=TASK_DEFINITION_ROOTS,
        help=(
            "Directories, task-definition YAML files, or .set files. "
            "Relative names are also resolved below test/programs and "
            "test/programs/simple."
        ),
    )
    parser.add_argument(
        "--require-language",
        action="store_true",
        help="Require options.language for all task definitions.",
    )
    parser.add_argument(
        "--require-properties",
        action="store_true",
        help="Require at least one property for all task definitions.",
    )
    parser.add_argument(
        "--check-property-files",
        action="store_true",
        help="Check that referenced property files exist.",
    )
    parser.add_argument(
        "--check-data-model",
        action="store_true",
        help="Check that specified data models are supported for the task language.",
    )
    return parser.parse_args(argv)


def main(argv=None):
    if yaml is None:
        print(
            "ERROR: PyYAML is required for checking task definitions.", file=sys.stderr
        )
        return 2

    args = parse_args(argv)
    error_count = [0]
    checked_files = 0

    for root in (resolve_root(root, error_count) for root in args.roots):
        if not root.exists():
            report_error(error_count, root, "does not exist")
            continue
        for task_definition in task_definition_files(root, error_count):
            checked_files += 1
            check_yaml_file(task_definition, error_count, args)

    print(
        "Checked {} YAML file(s), found {} error(s).".format(
            checked_files, error_count[0]
        )
    )
    return 1 if error_count[0] else 0


if __name__ == "__main__":
    sys.exit(main())
