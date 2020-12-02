#!/usr/bin/python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2017 Rodrigo Castano
# SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import logging
import os
import os.path
import shutil
import time
import unittest
import unittest.mock
from unittest.mock import patch

import post_processing.coverage.generate_coverage as generate_coverage

script_path = os.path.dirname(os.path.realpath(__file__))


class TestCoverage(unittest.TestCase):
    aux_root = os.path.join(script_path, "aux_files", "blocks")
    cpachecker_root = os.path.join(
        script_path, os.pardir, os.pardir, os.pardir, os.pardir
    )
    default_spec = os.path.join(
        cpachecker_root, "config", "specification", "ErrorLabel.spc"
    )
    default_timelimit = 10
    temp_folder = os.path.join(script_path, "temp_folder")

    def setUp(self):
        shutil.rmtree(self.temp_folder, ignore_errors=True)
        self.logger = logging.getLogger()
        self.logger.setLevel(logging.INFO)

    def tearDown(self):
        shutil.rmtree(self.temp_folder, ignore_errors=True)


class TestCoverageWhile(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "while.c")
        aa_file = os.path.join(self.aux_root, os.pardir, "true_aa.spc")
        start_time = time.time()
        with patch.object(self.logger, "info"):
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()

        self.assertEqual(lines_covered, {12, 13})
        self.assertEqual(lines_to_cover, {12, 13, 14, 15, 16})


class TestCoverageIf(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "if.c")
        aa_file = os.path.join(self.aux_root, os.pardir, "true_aa.spc")
        start_time = time.time()
        with patch.object(self.logger, "info"):
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()

        self.assertEqual(lines_covered, {12, 13})
        self.assertEqual(lines_to_cover, {12, 13, 14, 15, 16})


class TestCoverageSwitch(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "switch.c")
        aa_file = os.path.join(self.aux_root, os.pardir, "true_aa.spc")
        start_time = time.time()
        with patch.object(self.logger, "info"):
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()

        self.assertEqual(lines_covered, {12, 13, 14, 17, 18, 23, 24, 25})
        self.assertEqual(
            lines_to_cover,
            {12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30},
        )


class TestCoverageWhileWithCall(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "while_with_call.c")
        aa_file = os.path.join(self.aux_root, os.pardir, "true_aa.spc")
        start_time = time.time()
        with patch.object(self.logger, "info"):
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()

        self.assertEqual(lines_covered, {13, 14})
        self.assertEqual(lines_to_cover, {13, 14, 15, 16, 17, 18, 23, 24, 25, 26})


class TestCoverageMultilineSwitchExpression(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "multiline_switch.c")
        aa_file = os.path.join(self.aux_root, os.pardir, "true_aa.spc")
        start_time = time.time()
        with patch.object(self.logger, "info"):
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()

        self.assertEqual(
            lines_covered, {12, 13, 14, 15, 16, 17, 23, 24, 25, 26, 27, 32, 33, 34}
        )
        self.assertEqual(lines_to_cover, set(range(12, 40)) - {22})


class TestCoverageMultilineSwitchExpressionAndCall(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "multiline_switch_and_call.c")
        aa_file = os.path.join(self.aux_root, os.pardir, "true_aa.spc")
        start_time = time.time()
        with patch.object(self.logger, "info"):
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()
        # Compound expression in cond (lines 3-4) is considered as fully
        # evaluated even though line 4 cannot be reached.
        self.assertEqual(
            lines_covered,
            {12, 13, 16, 17, 18, 19, 20, 21, 27, 28, 29, 30, 31, 36, 37, 38},
        )
        self.assertEqual(lines_to_cover, {12, 13} | (set(range(16, 44)) - {26}))


if __name__ == "__main__":
    unittest.main()
