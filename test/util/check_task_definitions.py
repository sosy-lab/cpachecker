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

CHECK_WITNESS_TASKS = True

LANGUAGE_FILE_ENDINGS = {
    "C": (".c", ".i"),
    "Java": (".java",),
    "SV-LIB": (".svlib",),
}
LANGUAGES_ALLOWING_DIRECTORY_INPUTS = {"Java"}

SUPPORTED_DATA_MODELS = {
    "C": {"ILP32", "LP64"},
}

ADDITIONAL_FILE_KEYS = ("required_files",)


class Reporter:
    def __init__(self):
        self.errors = []

    def error(self, path, message):
        relative_path = path
        if path.is_absolute():
            relative_path = path.relative_to(CPACHECKER_DIR)

        self.errors.append("{}: {}".format(relative_path, message))

    def print_summary(self, checked_files):
        for error in self.errors:
            print("ERROR: " + error)
        print(
            "Checked {} YAML file(s), found {} error(s).".format(
                checked_files, len(self.errors)
            )
        )


def _normalize_to_list(value):
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]


def _load_yaml(path, reporter):
    try:
        with path.open(encoding="utf-8") as yml_file:
            return yaml.safe_load(yml_file)
    except yaml.YAMLError as exception:
        reporter.error(path, "invalid YAML: {}".format(exception))
        return None


def _is_witness_document(content):
    return isinstance(content, list) and any(
        isinstance(entry, dict) and "entry_type" in entry for entry in content
    )


def _witness_tasks(content):
    for entry in content:
        if not isinstance(entry, dict):
            continue
        metadata = entry.get("metadata")
        if isinstance(metadata, dict):
            task = metadata.get("task")
            if isinstance(task, dict):
                yield task


def _resolve_reference(base_dir, reference):
    if not isinstance(reference, str):
        return None
    return (base_dir / reference).resolve()


def _check_referenced_files(path, base_dir, key, values, reporter):
    for reference in _normalize_to_list(values):
        resolved = _resolve_reference(base_dir, reference)
        if resolved is None:
            reporter.error(
                path, "{} contains non-string entry {!r}".format(key, reference)
            )
        elif not resolved.exists():
            reporter.error(
                path, "{} references missing file '{}'".format(key, reference)
            )


def _check_language(path, input_files, language, reporter):
    if language and language not in LANGUAGE_FILE_ENDINGS:
        reporter.error(path, "unsupported programming language '{}'".format(language))
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
            reporter.error(
                path,
                "input file '{}' does not match language '{}' with endings {}".format(
                    input_file, language, ", ".join(allowed_endings)
                ),
            )


def _check_required_language(path, language, reporter):
    if not language:
        reporter.error(path, "missing programming language in options.language")
        return
    if language not in LANGUAGE_FILE_ENDINGS:
        reporter.error(path, "unsupported programming language '{}'".format(language))


def _check_data_model(path, language, data_model, reporter):
    if not data_model:
        return

    supported_models = SUPPORTED_DATA_MODELS.get(language)
    if supported_models is None:
        reporter.error(
            path,
            "data_model is specified for language '{}' without configured models".format(
                language
            ),
        )
    elif data_model not in supported_models:
        reporter.error(
            path,
            "unsupported data_model '{}' for language '{}'".format(
                data_model, language
            ),
        )


def _check_properties(
    path, content, reporter, require_properties, check_property_files
):
    properties = content.get("properties")
    if not properties:
        if require_properties:
            reporter.error(path, "missing or empty properties")
        return
    if not isinstance(properties, list):
        reporter.error(path, "properties is not a list")
        return

    for property_definition in properties:
        if not isinstance(property_definition, dict):
            reporter.error(
                path, "invalid property definition {!r}".format(property_definition)
            )
            continue
        property_file = property_definition.get("property_file")
        if not property_file:
            reporter.error(path, "property definition without property_file")
            continue
        if not check_property_files:
            continue
        resolved = _resolve_reference(path.parent, property_file)
        if resolved is None or not resolved.exists():
            reporter.error(
                path, "property_file references missing file '{}'".format(property_file)
            )


def _check_task_definition(path, content, reporter, args):
    if not isinstance(content, dict):
        reporter.error(path, "expected mapping for task definition")
        return

    input_files = _normalize_to_list(content.get("input_files"))
    if not input_files:
        reporter.error(path, "missing or empty input_files")
    _check_referenced_files(path, path.parent, "input_files", input_files, reporter)

    for additional_file_key in ADDITIONAL_FILE_KEYS:
        _check_referenced_files(
            path,
            path.parent,
            additional_file_key,
            content.get(additional_file_key),
            reporter,
        )

    options = content.get("options")
    if options is None:
        options = {}
    if not isinstance(options, dict):
        reporter.error(path, "options is not a mapping")
        options = {}

    language = options.get("language")
    if args.require_language:
        _check_required_language(path, language, reporter)
    _check_language(path, input_files, language, reporter)
    if args.check_data_model:
        _check_data_model(path, language, options.get("data_model"), reporter)
    _check_properties(
        path, content, reporter, args.require_properties, args.check_property_files
    )


def _check_witness(path, content, reporter, args):
    if not CHECK_WITNESS_TASKS:
        return

    tasks = list(_witness_tasks(content))
    if not tasks:
        reporter.error(path, "witness has no metadata.task section")
        return

    for task in tasks:
        input_files = _normalize_to_list(task.get("input_files"))
        if not input_files:
            reporter.error(path, "witness task has missing or empty input_files")
        _check_referenced_files(
            path, path.parent, "witness task input_files", input_files, reporter
        )

        language = task.get("language")
        if args.require_language:
            _check_required_language(path, language, reporter)
        _check_language(path, input_files, language, reporter)
        if args.check_data_model:
            _check_data_model(path, language, task.get("data_model"), reporter)

        if not task.get("specification"):
            reporter.error(path, "witness task has missing or empty specification")


def _check_yaml_file(path, reporter, args):
    content = _load_yaml(path, reporter)
    if content is None:
        return

    if _is_witness_document(content):
        _check_witness(path, content, reporter, args)
    else:
        _check_task_definition(path, content, reporter, args)


def _task_definition_files(root):
    for path in sorted(root.rglob("*.yml")):
        yield path
    for path in sorted(root.rglob("*.yaml")):
        yield path


def _parse_args(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "roots",
        nargs="*",
        type=Path,
        default=TASK_DEFINITION_ROOTS,
        help="Directories with task-definition YAML files.",
    )
    parser.add_argument(
        "--require-language",
        action="store_true",
        help="Require options.language for all task definitions.",
    )
    parser.add_argument(
        "--require-properties",
        action="store_true",
        help="Require at least one property for all non-witness task definitions.",
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

    args = _parse_args(argv)
    reporter = Reporter()
    checked_files = 0

    for root in args.roots:
        if not root.is_absolute():
            root = CPACHECKER_DIR / root
        if not root.is_dir():
            reporter.error(root, "not a directory")
            continue
        for task_definition in _task_definition_files(root):
            checked_files += 1
            _check_yaml_file(task_definition, reporter, args)

    reporter.print_summary(checked_files)
    return 1 if reporter.errors else 0


if __name__ == "__main__":
    sys.exit(main())
