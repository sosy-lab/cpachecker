#!/usr/bin/python3

import logging
import os
import os.path
import shutil
import sys
import unittest
import unittest.mock
from io import StringIO
from unittest.mock import MagicMock, call, patch

import post_processing.coverage.generate_coverage as generate_coverage

script_path = os.path.dirname(os.path.realpath(__file__))

def gen_files_in_dir(dir):
    for f in os.listdir(dir):
        yield os.path.join(dir, f)

class TestCoverage(unittest.TestCase):
    aux_root = os.path.join(script_path, 'aux_files')
    cpachecker_root = os.path.join(
        script_path, os.pardir, os.pardir, os.pardir, os.pardir)
    default_spec = os.path.join(
        cpachecker_root, 'config', 'specification', 'ErrorLabel.spc')
    default_timelimit = 10
    temp_folder = os.path.join(script_path, 'temp_folder')
    def setUp(self):
        try:
            shutil.rmtree(self.temp_folder)
        except:
            pass
        self.logger = logging.getLogger()
        self.logger.setLevel(logging.INFO)
    def tearDown(self):
        try:
            shutil.rmtree(self.temp_folder)
        except:
            pass

class TestGenerateExecutions(TestCoverage):
    pass

class TestGenerateOnlyPossibleExecution(TestGenerateExecutions):
    def test(self):
        instance = os.path.join(self.aux_root, 'two_loop_iterations.c')
        cex_count = 2 # Will only produce one, checking the output though.
        aa_file = os.path.join(self.aux_root, 'dummy_aa.spc')
        with patch.object(self.logger, 'info') as mock_logger:
            g = generate_coverage.GenerateFirstThenCollect(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=cex_count,
                spec=self.default_spec,
                heap_size=None,
                timelimit=self.default_timelimit,
                logger=self.logger,
                aa_file=aa_file)
            cex_generated = len(list(g.generate_executions()))
            mock_logger.assert_called_once_with('Generated 1 executions.')

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 1)
        self.assertEqual(cex_generated, 1)

class TestGenerateExceptionFoundBug(TestGenerateExecutions):
    def test(self):
        instance = os.path.join(self.aux_root, 'contains_error.c')
        cex_count = 2 # Will only produce one, checking the output though.
        aa_file = os.path.join(self.aux_root, 'dummy_aa.spc')
        with patch.object(self.logger, 'error') as mock_logger, \
             patch.object(self.logger, 'info') as mock_info:
            try:
                g = generate_coverage.GenerateFirstThenCollect(
                    instance=instance,
                    output_dir=self.temp_folder,
                    cex_count=cex_count,
                    spec=self.default_spec,
                    heap_size=None,
                    timelimit=self.default_timelimit,
                    logger=self.logger,
                    aa_file=aa_file)
                cex_generated = len(list(g.generate_executions()))
                self.fail('Should have raised FoundBugException.')
            except:
                pass
            mock_logger.assert_called_once_with(
                'Found an assertion violation. '
                'Inspect counterexamples before collecting a '
                'coverage measure.')
            mock_info.assert_called_once_with('Generated 2 executions.')

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 2)
        # Method does not reach the return statement. Cannot check
        # reported number of generated counterexamples.

class TestGenerateAllPaths(TestGenerateExecutions):
    def test(self):
        instance = os.path.join(self.aux_root, 'three_paths.c')
        cex_count = 10 # There are only 3 paths though, checking the output.
        aa_file = os.path.join(self.aux_root, 'dummy_aa.spc')
        with patch.object(self.logger, 'info') as mock_info:
            g = generate_coverage.GenerateFirstThenCollect(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=cex_count,
                spec=self.default_spec,
                heap_size=None,
                timelimit=self.default_timelimit,
                logger=self.logger,
                aa_file=aa_file)
            cex_generated = len(list(g.generate_executions()))
            mock_info.assert_called_once_with('Generated 3 executions.')

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 3)
        self.assertEqual(cex_generated, 3)

class TestDocumentExpectedShortcoming(TestGenerateExecutions):
    def test(self):
        '''
        This is known behavior that is worth to document. We will only get
        a single execution for each return statement due to how states
        are merged.
        '''
        instance = os.path.join(self.aux_root, 'one_per_return.c')
        cex_count = 10
        aa_file = os.path.join(self.aux_root, 'dummy_aa.spc')
        with patch.object(self.logger, 'info') as mock_info:
            g = generate_coverage.GenerateFirstThenCollect(
                instance=instance,
                output_dir=self.temp_folder,
                cex_count=cex_count,
                spec=self.default_spec,
                heap_size=None,
                timelimit=self.default_timelimit,
                logger=self.logger,
                aa_file=aa_file)
            cex_generated = len(list(g.generate_executions()))
            mock_info.assert_called_once_with('Generated 1 executions.')

        self.assertTrue(os.path.exists(self.temp_folder))
        self.assertEqual(len(os.listdir(self.temp_folder)), 1)
        self.assertEqual(cex_generated, 1)

class TestCoverageAAIsPrefix(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, 'three_paths.c')
        aa_file = os.path.join(
            self.aux_root, 'aa_three_paths_else_return_not_covered.spc')
        specs_dir = os.path.join(
            self.aux_root, 'cex_three_paths', 'outer_else_block')
        
        with patch.object(self.logger, 'info') as mock_info:
            self.logger.setLevel(logging.DEBUG)
            c = generate_coverage.CollectFromExistingExecutions(
                instance=instance,
                cex_dir=specs_dir,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file)
            lines_covered, lines_to_cover = \
                c.collect_coverage()
            expected_calls =  [
                call('Coverage after collecting 1 executions:'),
                call('Lines covered: 3'),
                call('Total lines to cover: 10'),
                call(''),
                call('Total lines covered: 3'),
                call('Total lines to cover: 10')
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

        self.assertEqual(lines_covered, set([3,4,13]))
        self.assertEqual(lines_to_cover, set([3,4,5,6,7,9,10,13,14,15]))

class TestCoverageTreeAAAnd2Paths(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, 'three_paths.c')
        aa_file = os.path.join(
            self.aux_root, 'aa_three_paths_inner_if_both_blocks.spc')
        specs_dir = os.path.join(
            self.aux_root, 'cex_three_paths', 'inner_both_blocks')
        with patch.object(self.logger, 'info') as mock_info:
            c = generate_coverage.CollectFromExistingExecutions(
                instance=instance,
                cex_dir=specs_dir,
                heap_size=None,
                timelimit=None,
                logger=self.logger,
                aa_file=aa_file)
            lines_covered, lines_to_cover = \
                c.collect_coverage()
            expected_calls =  [
                call('Coverage after collecting 1 executions:'),
                call('Lines covered: 4'),
                call('Total lines to cover: 10'),
                call(''),
                call('Coverage after collecting 2 executions:'),
                call('Lines covered: 5'),
                call('Total lines to cover: 10'),
                call(''),
                call('Total lines covered: 5'),
                call('Total lines to cover: 10')
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

        self.assertEqual(lines_covered, set([3,4,5,6,9]))
        self.assertEqual(lines_to_cover, set([3,4,5,6,7,9,10,13,14,15]))

class TestCoverageIntegrationOnlyCollectCoverage(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, 'three_paths.c')
        aa_file = os.path.join(
            self.aux_root, 'aa_three_paths_inner_if_both_blocks.spc')
        specs_dir = os.path.join(
            self.aux_root, 'cex_three_paths', 'inner_both_blocks')
        argv = [ str(x) for x in [
            '-assumption_automaton_file', aa_file,
            '-cex_dir', specs_dir,
            '-only_collect_coverage',
            '-spec', self.default_spec,
            instance
        ]]
        with patch.object(self.logger, 'info') as mock_info:
            generate_coverage.main(argv, self.logger)
            expected_calls =  [
                call('Coverage after collecting 1 executions:'),
                call('Lines covered: 4'),
                call('Total lines to cover: 10'),
                call(''),
                call('Coverage after collecting 2 executions:'),
                call('Lines covered: 5'),
                call('Total lines to cover: 10'),
                call(''),
                call('Total lines covered: 5'),
                call('Total lines to cover: 10')
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

class TestCoverageIntegrationTimelimitOptional(TestCoverage):
    def test(self):
        instance = os.path.join(self.aux_root, 'three_paths.c')
        aa_file = os.path.join(
            self.aux_root, 'aa_three_paths_inner_if_both_blocks.spc')
        non_existent_dir = self.temp_folder
        argv = [ str(x) for x in [
            '-assumption_automaton_file', aa_file,
            '-cex_dir', non_existent_dir,
            '-spec', self.default_spec,
            '-cex_count', 10,
            instance
        ]]
        with patch.object(self.logger, 'info') as mock_info:
            generate_coverage.main(argv, self.logger)
            expected_calls =  [
                call('Generated 3 executions.'),
                call('Coverage after collecting 1 executions:'),
                call('Lines covered: 3'),
                call('Total lines to cover: 10'),
                call(''),
                call('Coverage after collecting 2 executions:'),
                call('Lines covered: 5'),
                call('Total lines to cover: 10'),
                call(''),
                call('Coverage after collecting 3 executions:'),
                call('Lines covered: 6'),
                call('Total lines to cover: 10'),
                call(''),
                call('Total lines covered: 6'),
                call('Total lines to cover: 10')
            ]
            self.assertEqual(mock_info.mock_calls, expected_calls)

class TestOutputParsingTrue(unittest.TestCase):
    output = """Error path found and confirmed by counterexample check with CPACHECKER. (CounterexampleCheckAlgorithm.checkCounterexample, INFO)\n\nStopping analysis ... (CPAchecker.runAlgorithm, INFO)\n\nVerification result: FALSE. Property violation (Found covering test case) found by chosen configuration.\nMore details about the verification run can be found in the directory "/home/doc/files/tools/cpachecker/svn/scripts/post_processing/coverage/temp_dir_coverage".\nGraphical representation included in the "Report.html" file."""
    def test(self):
        self.assertTrue(
            generate_coverage.only_generated_successful_executions(
                self.output))

class TestOutputParsingFalse(unittest.TestCase):
    output = """Error path found and confirmed by counterexample check with CPACHECKER. (CounterexampleCheckAlgorithm.checkCounterexample, INFO)\n\nStopping analysis ... (CPAchecker.runAlgorithm, INFO)\n\nVerification result: FALSE. Property violation (Found covering test case, some error in line 4) found by chosen configuration.\nMore details about the verification run can be found in the directory "/home/doc/files/tools/cpachecker/svn/scripts/post_processing/coverage/temp_dir_coverage".\nGraphical representation included in the "Report.html" file."""
    def test(self):
        self.assertFalse(
            generate_coverage.only_generated_successful_executions(
                self.output))

if __name__ == '__main__':
    unittest.main()
