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
from unittest.mock import call, patch

import post_processing.coverage.generate_coverage as generate_coverage

script_path = os.path.dirname(os.path.realpath(__file__))


class TestCoverage(unittest.TestCase):
    aux_root = os.path.join(script_path, "aux_files")
    cpachecker_root = os.path.join(
        script_path, os.pardir, os.pardir, os.pardir, os.pardir
    )
    default_spec = os.path.join(
        cpachecker_root, "config", "specification", "ErrorLabel.spc"
    )
    default_timelimit = 20
    temp_folder = os.path.join(script_path, "temp_folder")

    def setUp(self):
        shutil.rmtree(self.temp_folder, ignore_errors=True)
        self.logger = logging.getLogger()
        self.logger.setLevel(logging.INFO)
        self.start_time = time.time()

    def tearDown(self):
        shutil.rmtree(self.temp_folder, ignore_errors=True)


class TestGenerateExecutions(TestCoverage):
    pass


class TestGenerateOnlyPossibleExecution(TestGenerateExecutions):
    def test(self):
        instance = os.path.join(self.aux_root, "two_loop_iterations.c")
        cex_count = 2  # Will only produce one, checking the output though.
        aa_file = os.path.join(self.aux_root, "dummy_aa.spc")
        with patch.object(self.logger, "info") as mock_logger:
            g = generate_coverage.GenerateFirstThenCollect(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=cex_count,
                spec=self.default_spec,
                heap_size=None,
                timelimit=self.default_timelimit,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            cex_generated = len(list(g.generate_executions()))
            mock_logger.assert_called_once_with("Generated 1 executions.")

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 1)
        self.assertEqual(cex_generated, 1)


class TestGenerateExceptionFoundBug(TestGenerateExecutions):
    def test(self):
        instance = os.path.join(self.aux_root, "contains_error.c")
        cex_count = 2  # Will only produce one, checking the output though.
        aa_file = os.path.join(self.aux_root, "dummy_aa.spc")

        with patch.object(self.logger, "error") as mock_logger, patch.object(
            self.logger, "info"
        ) as mock_info:
            try:
                g = generate_coverage.GenerateFirstThenCollect(
                    instance=instance,
                    output_dir=self.temp_folder,
                    cex_count=cex_count,
                    spec=self.default_spec,
                    heap_size=None,
                    timelimit=self.default_timelimit,
                    logger=self.logger,
                    aa_file=aa_file,
                    start_time=self.start_time,
                    timer=generate_coverage.Timer(),
                )
                list(g.generate_executions())
                self.fail("Should have raised FoundBugError.")
            except generate_coverage.FoundBugError:
                pass
            mock_logger.assert_called_once_with(
                "Found an assertion violation. "
                "Inspect counterexamples before collecting a "
                "coverage measure."
            )
            mock_info.assert_called_once_with("Generated 2 executions.")

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 2)
        # Method does not reach the return statement. Cannot check
        # reported number of generated counterexamples.


class TestGenerateAllPaths(TestGenerateExecutions):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        cex_count = 10  # There are only 3 paths though, checking the output.
        aa_file = os.path.join(self.aux_root, "dummy_aa.spc")
        with patch.object(self.logger, "info") as mock_info:
            g = generate_coverage.GenerateFirstThenCollect(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=cex_count,
                spec=self.default_spec,
                heap_size=None,
                timelimit=self.default_timelimit,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            cex_generated = len(list(g.generate_executions()))
            mock_info.assert_called_once_with("Generated 3 executions.")

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 3)
        self.assertEqual(cex_generated, 3)


class TestDocumentExpectedShortcoming(TestGenerateExecutions):
    def test(self):
        """
        This is known behavior that is worth to document. We will only get
        a single execution for each return statement due to how states
        are merged.
        """
        instance = os.path.join(self.aux_root, "one_per_return.c")
        cex_count = 10
        aa_file = os.path.join(self.aux_root, "dummy_aa.spc")
        with patch.object(self.logger, "info") as mock_info:
            g = generate_coverage.GenerateFirstThenCollect(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=cex_count,
                spec=self.default_spec,
                heap_size=None,
                timelimit=self.default_timelimit,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            cex_generated = len(list(g.generate_executions()))
            mock_info.assert_called_once_with("Generated 1 executions.")

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 1)
        self.assertEqual(cex_generated, 1)


class TestCoverageAAIsPrefixFromExistingPath(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(
            self.aux_root, "aa_three_paths_else_return_not_covered.spc"
        )
        specs_dir = os.path.join(self.aux_root, "cex_three_paths", "outer_else_block")

        with patch.object(self.logger, "info") as mock_info:
            c = generate_coverage.CollectFromExistingExecutions(
                instance=instance,
                cex_dir=specs_dir,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()
            expected_calls = [
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 3),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Total lines covered: %s", 3),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

        self.assertEqual(lines_covered, {12, 13, 22})
        self.assertEqual(lines_to_cover, {12, 13, 14, 15, 16, 18, 19, 22, 23, 24})


class TestCoveragePathAAFixPoint(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(
            self.aux_root, "aa_three_paths_else_return_not_covered.spc"
        )
        with patch.object(self.logger, "info") as mock_info:
            c = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()
            expected_calls = [
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 3),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 0 executions."),
                call("Total lines covered: %s", 3),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

        self.assertEqual(lines_covered, {12, 13, 22})
        self.assertEqual(lines_to_cover, {12, 13, 14, 15, 16, 18, 19, 22, 23, 24})


class TestCoverageTreeAAAndExisting2Paths(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        specs_dir = os.path.join(self.aux_root, "cex_three_paths", "inner_both_blocks")
        with patch.object(self.logger, "info") as mock_info:
            c = generate_coverage.CollectFromExistingExecutions(
                instance=instance,
                cex_dir=specs_dir,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            lines_covered, lines_to_cover = c.collect_coverage()
            expected_calls = [
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 4),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Coverage after collecting %s executions:", 2),
                call("Lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Total lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

        self.assertEqual(lines_covered, {12, 13, 14, 15, 18})
        self.assertEqual(lines_to_cover, {12, 13, 14, 15, 16, 18, 19, 22, 23, 24})


class TestCoverageFixPointProducesExecutions(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "single_true_state.spc")
        specs_dir = self.temp_folder
        with patch.object(self.logger, "info") as mock_info:
            g = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=specs_dir,
                cex_count=1,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            cex_generated = list(g.generate_executions())
            expected_calls = [call("Generated 1 executions.")]
            self.assertEqual(mock_info.mock_calls, expected_calls)
        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 2)
        self.assertEqual(len(cex_generated), 1)


class TestCoverageFixPointProducesAllPossibleExecutions(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "single_true_state.spc")
        specs_dir = self.temp_folder
        with patch.object(self.logger, "info") as mock_info:
            g = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=specs_dir,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            cex_generated = [next(g.generate_executions())]
            # Updating covered lines, to force the generator to cover
            # other lines.
            g.lines_covered.update([12, 13, 22, 23, 24])
            cex_generated.append(next(g.generate_executions()))
            g.lines_covered.update([12, 13, 14, 15, 16])
            cex_generated.append(next(g.generate_executions()))
            g.lines_covered.update([12, 13, 14, 18, 19])
            expected_calls = [
                call("Generated 1 executions."),
                call("Generated 1 executions."),
                call("Generated 1 executions."),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)
            self.assertEqual(len(list(g.generate_executions())), 0)
        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 6)
        self.assertEqual(len(cex_generated), 3)


class TestCoverageFixPointWithinAssumptionAutomatonPath(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        specs_dir = self.temp_folder
        with patch.object(self.logger, "info") as mock_info:
            g = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=specs_dir,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            # Updating covered lines, to force the generator to cover
            # the only other possible path.
            g.lines_covered.update([12, 13, 14, 18])
            cex_generated = [next(g.generate_executions())]
            # Updating covered lines, to reflect the execution just produced.
            g.lines_covered.update([12, 13, 14, 15])
            cex_generated += list(g.generate_executions())
            expected_calls = [
                call("Generated 1 executions."),
                call("Generated 0 executions."),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)
        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 2)
        self.assertEqual(len(cex_generated), 1)


class TestCoverageFixPointAlreadyReached(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        specs_dir = self.temp_folder
        with patch.object(self.logger, "info") as mock_info:
            g = generate_coverage.FixPointOnCoveredLines(
                instance=instance,
                output_dir=specs_dir,
                cex_count=10,
                spec=self.default_spec,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file,
                start_time=self.start_time,
                timer=generate_coverage.Timer(),
            )
            # Updating covered lines such that it is impossible to
            # cover more lines.
            g.lines_covered.update([12, 13, 14, 15, 18])
            cex_generated = list(g.generate_executions())
            expected_calls = [call("Generated 0 executions.")]
            self.assertEqual(mock_info.mock_calls, expected_calls)
        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 0)
        self.assertEqual(len(cex_generated), 0)


class TestCoverageIntegrationOnlyCollectCoverage(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        specs_dir = os.path.join(self.aux_root, "cex_three_paths", "inner_both_blocks")
        argv = [
            str(x)
            for x in [
                "-assumption_automaton_file",
                aa_file,
                "-cex_dir",
                specs_dir,
                "-only_collect_coverage",
                "-spec",
                self.default_spec,
                "-generator_type",
                "blind",
                instance,
            ]
        ]
        with patch.object(self.logger, "info") as mock_info:
            generate_coverage.main(argv, self.logger)
            expected_calls = [
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 4),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Coverage after collecting %s executions:", 2),
                call("Lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Total lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)


class TestCoverageIntegrationTimelimitOptional(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        non_existent_dir = self.temp_folder
        argv = [
            str(x)
            for x in [
                "-assumption_automaton_file",
                aa_file,
                "-cex_dir",
                non_existent_dir,
                "-spec",
                self.default_spec,
                "-cex_count",
                10,
                "-generator_type",
                "blind",
                instance,
            ]
        ]
        with patch.object(self.logger, "info") as mock_info:
            generate_coverage.main(argv, self.logger)
            expected_calls = [
                call("Generated 3 executions."),
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 1),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Coverage after collecting %s executions:", 2),
                call("Lines covered: %s", 4),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Coverage after collecting %s executions:", 3),
                call("Lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Total lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)


class TestCoverageIntegrationFixPoint(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        non_existent_dir = self.temp_folder
        argv = [
            str(x)
            for x in [
                "-assumption_automaton_file",
                aa_file,
                "-cex_dir",
                non_existent_dir,
                "-spec",
                self.default_spec,
                "-cex_count",
                10,
                "-generator_type",
                "fixpoint",
                instance,
            ]
        ]
        with patch.object(self.logger, "info") as mock_info:
            generate_coverage.main(argv, self.logger)
            expected_calls = [
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 1),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 2),
                call("Lines covered: %s", 4),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 3),
                call("Lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 0 executions."),
                call("Total lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)


class TestOutputParsingNoBugFound(unittest.TestCase):
    output = """Error path found and confirmed by counterexample check with CPACHECKER. (CounterexampleCheckAlgorithm.checkCounterexample, INFO)\n\nStopping analysis ... (CPAchecker.runAlgorithm, INFO)\n\nVerification result: FALSE. Property violation (Found covering test case) found by chosen configuration.\nMore details about the verification run can be found in the directory "/home/doc/files/tools/cpachecker/svn/scripts/post_processing/coverage/temp_dir_coverage".\nGraphical representation included in the "Report.html" file."""

    def test(self):
        logger = logging.getLogger()
        with patch.object(logger, "error") as mock_error:
            cpachecker_result = generate_coverage.parse_result(self.output, logger)
            self.assertEqual(mock_error.mock_calls, [])
        self.assertFalse(cpachecker_result.found_bug())
        self.assertTrue(cpachecker_result.found_property_violation())


class TestOutputParsingFoundBug(unittest.TestCase):
    output = """Error path found and confirmed by counterexample check with CPACHECKER. (CounterexampleCheckAlgorithm.checkCounterexample, INFO)\n\nStopping analysis ... (CPAchecker.runAlgorithm, INFO)\n\nVerification result: FALSE. Property violation (Found covering test case, some error in line 4) found by chosen configuration.\nMore details about the verification run can be found in the directory "/home/doc/files/tools/cpachecker/svn/scripts/post_processing/coverage/temp_dir_coverage".\nGraphical representation included in the "Report.html" file."""

    def test(self):
        logger = logging.getLogger()
        with patch.object(logger, "error") as mock_error:
            cpachecker_result = generate_coverage.parse_result(self.output, logger)
            self.assertEqual(mock_error.mock_calls, [])
        self.assertTrue(cpachecker_result.found_bug())
        self.assertTrue(cpachecker_result.found_property_violation())


class TestOutputParsingWithoutPropertyViolation(unittest.TestCase):
    output = """Error path found and confirmed by counterexample check with CPACHECKER. (CounterexampleCheckAlgorithm.checkCounterexample, INFO)\n\nStopping analysis ... (CPAchecker.runAlgorithm, INFO)\n\nVerification result: TRUE. No property violation found by chosen configuration.\nMore details about the verification run can be found in the directory "/home/doc/files/tools/cpachecker/svn/scripts/post_processing/coverage/temp_dir_coverage".\nGraphical representation included in the "Report.html" file."""

    def test(self):
        logger = logging.getLogger()
        with patch.object(logger, "error") as mock_error:
            cpachecker_result = generate_coverage.parse_result(self.output, logger)
            self.assertEqual(mock_error.mock_calls, [])
        self.assertFalse(cpachecker_result.found_property_violation())


class TestOutputParsingExceptionThrown(unittest.TestCase):
    output = """Error path found and confirmed by counterexample check with CPACHECKER. (CounterexampleCheckAlgorithm.checkCounterexample, INFO)\n\nStopping analysis ... (CPAchecker.runAlgorithm, INFO)\n\nVerification result: TRUE. No property violation found by chosen configuration.\nMore details about the verification run can be found in the directory "/home/doc/files/tools/cpachecker/svn/scripts/post_processing/coverage/temp_dir_coverage".\nGraphical representation included in the "Report.html" file."""

    def test(self):
        logger = logging.getLogger()
        with patch.object(logger, "error") as mock_error:
            cpachecker_result = generate_coverage.parse_result(self.output, logger)
            self.assertEqual(mock_error.mock_calls, [])

        with self.assertRaisesRegex(
            Exception, "This method should not have been called"
        ):
            cpachecker_result.found_bug()


class TestOutputParsingIncompleteOutput(unittest.TestCase):
    output = """Output ending prematurely... (result cannot be parsed)"""

    def test(self):
        logger = logging.getLogger()
        with patch.object(logger, "error") as mock_error:
            cpachecker_result = generate_coverage.parse_result(self.output, logger)
            mock_error.assert_called_with("Failed to parse CPAchecker output.")
        self.assertFalse(cpachecker_result.found_property_violation())


class TestCoverageIntegrationCexCountOptional(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "three_paths.c")
        aa_file = os.path.join(self.aux_root, "aa_three_paths_inner_if_both_blocks.spc")
        non_existent_dir = self.temp_folder
        argv = [
            str(x)
            for x in [
                "-assumption_automaton_file",
                aa_file,
                "-cex_dir",
                non_existent_dir,
                "-spec",
                self.default_spec,
                "-timelimit",
                str(900),
                "-generator_type",
                "fixpoint",
                instance,
            ]
        ]
        with patch.object(self.logger, "info") as mock_info:
            generate_coverage.main(argv, self.logger)
            expected_calls = [
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 1),
                call("Lines covered: %s", 1),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 2),
                call("Lines covered: %s", 4),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 1 executions."),
                call("Coverage after collecting %s executions:", 3),
                call("Lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
                call(""),
                call("Generated 0 executions."),
                call("Total lines covered: %s", 5),
                call("Total lines to cover: %s", 10),
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)


class MockAdd10SecTimer:
    def __init__(self, time_constant):
        self.time_constant = time_constant

    def time(self):
        self.time_constant += 10
        return self.time_constant


class TestCoverageIntegrationTimeout(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, "loop_many_paths.c")
        aa_file = os.path.join(self.aux_root, "true_aa.spc")
        non_existent_dir = self.temp_folder
        timelimit = 20
        argv = [
            str(x)
            for x in [
                "-assumption_automaton_file",
                aa_file,
                "-cex_dir",
                non_existent_dir,
                "-spec",
                self.default_spec,
                "-timelimit",
                str(timelimit),
                "-generator_type",
                "fixpoint",
                instance,
            ]
        ]
        start_time = time.time()
        lines_covered = None
        log = []

        def side_effect(info_msg, *args):
            log.append(info_msg % tuple(args))

            nonlocal lines_covered
            if info_msg == "Total lines covered: %s":
                assert isinstance(args[0], int)
                lines_covered = args[0]

        # Will allow only one terminating execution to be generated,
        # since the timer adds 10 second for each call to method 'time'.
        timer = MockAdd10SecTimer(start_time)
        with patch.object(self.logger, "info") as mock_info, patch.object(
            self.logger, "error"
        ) as mock_error:
            mock_info.side_effect = side_effect
            generate_coverage.main(argv, self.logger, timer=timer)
            self.assertEqual(mock_error.mock_calls, [])
        elapsed_time = time.time() - start_time
        self.assertGreater(
            2 * timelimit,
            elapsed_time,
            msg="Timeout occured, log was:\n" + "\n".join(log),
        )
        self.assertGreater(
            lines_covered,
            0,
            msg="Unexpected negative number for line coverage, log was:\n"
            + "\n".join(log),
        )
        self.assertGreater(
            25,
            lines_covered,
            msg="Unexpectedly low number of covered lines , log was:\n"
            + "\n".join(log),
        )


if __name__ == "__main__":
    unittest.main()
