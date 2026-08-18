#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""Tests for the task-definition consistency checks."""

import copy
from pathlib import Path
import tempfile
import unittest

import check_task_definitions as checker


class TaskDefinitionValidationTest(unittest.TestCase):
    """Test validation of individual task definitions and their inputs."""

    def setUp(self):
        """Create an isolated valid C benchmark for each test."""
        checker.errors.clear()
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.directory = Path(self.temporary_directory.name)
        (self.directory / "program.c").touch()
        (self.directory / "property.prp").touch()
        self.definition_path = self.directory / "task.yml"
        self.definition = {
            "format_version": "2.1",
            "input_files": "program.c",
            "properties": [
                {
                    "property_file": "property.prp",
                    "expected_verdict": True,
                }
            ],
            "options": {"language": "C", "data_model": "LP64"},
        }

    def tearDown(self):
        """Remove temporary files and errors created by a test."""
        checker.errors.clear()
        self.temporary_directory.cleanup()

    def validate(self, definition=None):
        """Validate a definition and return its error-message strings."""
        checker.errors.clear()
        checker.check_task_definition(
            self.definition_path,
            self.definition if definition is None else definition,
        )
        return {error.error_type for error in checker.errors}

    def test_valid_c_definition(self):
        """Accept a valid C task definition."""
        self.assertFalse(self.validate())

    def test_valid_java_and_svlib_definitions(self):
        """Accept valid Java-directory and SV-LIB inputs."""
        for language, input_file in (("Java", "program"), ("SV-LIB", "program.svlib")):
            with self.subTest(language=language):
                input_path = self.directory / input_file
                if language == "Java":
                    input_path.mkdir()
                else:
                    input_path.touch()

                definition = copy.deepcopy(self.definition)
                definition["input_files"] = input_file
                definition["options"] = {"language": language}
                self.assertFalse(self.validate(definition))

    def test_language_is_required_and_must_match_input(self):
        """Reject missing, invalid, unsupported, and mismatching languages."""
        cases = (
            (None, "missing programming language"),
            ([], "programming language contains non-string entry []"),
            ("Python", "unsupported programming language 'Python'"),
            (
                "Java",
                "input file 'program.c' does not match language 'Java' "
                "with endings .java",
            ),
        )
        for language, expected_error in cases:
            with self.subTest(language=language):
                definition = copy.deepcopy(self.definition)
                definition["options"] = {"language": language}
                self.assertIn(expected_error, self.validate(definition))

    def test_data_model_must_be_supported_string(self):
        """Reject non-string and unsupported C data models."""
        for data_model, expected_error in (
            ([], "data_model contains non-string entry []"),
            ("ILP64", "unsupported data_model 'ILP64' for language 'C'"),
        ):
            with self.subTest(data_model=data_model):
                definition = copy.deepcopy(self.definition)
                definition["options"]["data_model"] = data_model
                self.assertIn(expected_error, self.validate(definition))

    def test_properties_are_required(self):
        """Reject missing and empty property lists."""
        definition = copy.deepcopy(self.definition)
        del definition["properties"]
        self.assertEqual({"missing properties"}, self.validate(definition))

        definition["properties"] = []
        self.assertEqual({"empty properties"}, self.validate(definition))

    def test_yaml_field_types_are_validated(self):
        """Report malformed field values without raising an exception."""
        definition = {
            "format_version": 2.0,
            "input_files": [[]],
            "properties": [
                {
                    "property_file": [],
                    "expected_verdict": "true",
                    "subproperty": [],
                }
            ],
            "options": {"language": [], "data_model": []},
        }
        error_messages = self.validate(definition)
        self.assertEqual(7, len(error_messages))
        self.assertIn("format_version contains non-string entry 2.0", error_messages)
        self.assertIn(
            "expected_verdict contains non-boolean entry 'true'", error_messages
        )

    def test_memsafety_subproperty(self):
        """Require a supported sub-property for false MemSafety verdicts."""
        (self.directory / checker.MEMSAFETY_PROPERTY_FILE).touch()
        definition = copy.deepcopy(self.definition)
        property_definition = definition["properties"][0]
        property_definition["property_file"] = checker.MEMSAFETY_PROPERTY_FILE
        property_definition["expected_verdict"] = False

        self.assertIn(
            "false MemSafety property without subproperty", self.validate(definition)
        )

        property_definition["subproperty"] = "valid-access"
        self.assertIn(
            "unsupported MemSafety subproperty 'valid-access'",
            self.validate(definition),
        )

        property_definition["subproperty"] = "valid-deref"
        self.assertFalse(self.validate(definition))

    def test_referenced_files_must_exist(self):
        """Reject missing benchmark inputs and property files."""
        definition = copy.deepcopy(self.definition)
        definition["input_files"] = "missing.c"
        definition["properties"][0]["property_file"] = "missing.prp"
        error_messages = self.validate(definition)
        self.assertIn("input_files references missing file 'missing.c'", error_messages)
        self.assertIn(
            "property_file references missing file 'missing.prp'", error_messages
        )

    def test_invalid_yaml_is_reported(self):
        """Report invalid YAML instead of propagating a parser exception."""
        self.definition_path.write_text("properties: [", encoding="utf-8")
        checker.check_benchmark(self.definition_path)
        self.assertTrue(
            any(
                error.error_type.startswith("invalid YAML:")
                for error in checker.errors
            )
        )


if __name__ == "__main__":
    unittest.main()
