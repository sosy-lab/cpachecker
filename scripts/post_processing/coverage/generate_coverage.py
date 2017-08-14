#!/usr/bin/python3

from __future__ import print_function

import argparse
import errno
import glob
import json
import logging
import os
import os.path
import re
import shutil
import subprocess
import sys
from subprocess import check_output


class FoundBugException(Exception):
    pass

script_path = os.path.dirname(os.path.realpath(__file__))
cpachecker_root = os.path.join(script_path, os.pardir, os.pardir, os.pardir)

temp_dir = os.path.join(script_path, 'temp_dir_coverage')

def print_command(command, logger):
    for c in command[:-1]:
        logger.debug(c + " \\")
    logger.debug(command[-1])

def create_temp_dir(temp_dir):
    try:
        shutil.rmtree(temp_dir)
    except:
        pass
    os.makedirs(temp_dir)

coverage_test_case_message = 'Found covering test case'
# 
# def found_coverage_test_case(output):
#     return coverage_test_case_message.encode('utf-8') in output

def gen_reach_exit_spec(f):
    print('CONTROL AUTOMATON CoverageAutomaton', file=f)
    print('', file=f)
    print('INITIAL STATE WaitForExit;', file=f)
    print('', file=f)
    print('STATE USEFIRST WaitForExit:', file=f)
    print(
        '  MATCH EXIT -> ERROR("' + coverage_test_case_message + '");',
        file=f)
    print('', file=f)
    print('END AUTOMATON', file=f)

def create_spec(spec_folder):
    reach_exit_spec_file = os.path.join(spec_folder, 'spec.spc')
    with open(reach_exit_spec_file, 'w') as f:
        gen_reach_exit_spec(f)
    return reach_exit_spec_file

def cpachecker_command(
    temp_dir,
    specs,
    heap_size,
    cex_count,
    timelimit,
    instance,
    export_coverage=False):
    conf = os.path.join(
        cpachecker_root, 'config', 'valueAnalysis.properties')
    coverage_options = [
        '-setprop',
            'counterexample.export.exportCounterexampleCoverage=true',
        '-setprop',
            'cpa.composite.aggregateBasicBlocks=false']

    timelimit_prop = []
    if timelimit is not None:
        timelimit_prop = ['-setprop', 'limits.time.cpu='+str(timelimit)+'s']
    return [
        os.path.join(cpachecker_root, 'scripts', 'cpa.sh'),
        '-config' , conf,
        '-outputpath', temp_dir,
        '-setprop', 'specification=' + ','.join(specs)] + (
        ['-heap', heap_size] if heap_size else []) + [
        '-setprop', 'analysis.stopAfterError=false',
        '-setprop', 'analysis.counterexampleLimit='+str(cex_count),
        '-setprop', 'analysis.traversal.usePostorder=true'] + (
        timelimit_prop) + [
        '-setprop', 'analysis.traversal.order=DFS',
        '-setprop', 'analysis.traversal.useReversePostorder=false',
        '-setprop', 'analysis.traversal.useCallstack=false' ] + ( 
        coverage_options if export_coverage else []) + [
        instance]

def counterexample_spec_files(cex_dir):
    pattern = r'.*Counterexample.([^.]*).spc'
    all_files = sorted(os.listdir(cex_dir))
    return [os.path.join(cex_dir, cex)
            for cex in all_files if re.match(pattern, cex)]

def counterexample_coverage_files(cex_dir):
    pattern = r'.*Counterexample.([^.]*).aa-prefix.coverage-info'
    all_files = sorted(os.listdir(cex_dir))
    return [os.path.join(cex_dir, cex)
            for cex in all_files if re.match(pattern, cex)]

def move_execution_spec_files(temp_dir, output_dir):
    if os.path.exists(output_dir):
        msg = 'Output directory (' + output_dir + ') should not exist.'
        raise ValueError(msg)
    os.makedirs(output_dir)
    all_cex_specs = counterexample_spec_files(temp_dir)
    for spec in all_cex_specs:
        shutil.copy(src=spec, dst=output_dir)

def run_command(command, logger):
    logger.debug("Executing:")
    print_command(command, logger)
    try:
        output = check_output(command, stderr=subprocess.STDOUT)
    except subprocess.CalledProcessError as e:
        logger.error(e.output)
        raise e
    logger.debug("Finished Executing")
    logger.debug(output)
    return output

def only_generated_successful_executions(output):
    pattern = r'Verification result: [^(]*\((?P<message>[^)]*)\).*'
    m = re.search(pattern=pattern, string=str(output))
    if not m:
        raise Exception("Failed to parse CPAchecker output.")
    return m.group('message') == coverage_test_case_message

def generate_executions(
    instance,
    output_dir,
    cex_count,
    spec,
    heap_size,
    timelimit,
    logger):

    create_temp_dir(temp_dir)
    reach_exit_spec_file = create_spec(spec_folder=temp_dir)

    specs = [reach_exit_spec_file, spec]

    # This configuration seems to produce sufficiently good executions.
    # However, it is entirely possible to change it.

    command = cpachecker_command(
        temp_dir=temp_dir,
        specs=specs,
        heap_size=heap_size,
        cex_count=cex_count,
        timelimit=timelimit,
        instance=instance)

    try:
        output = run_command(command, logger)
        bug_found = not only_generated_successful_executions(output)
        move_execution_spec_files(temp_dir=temp_dir, output_dir=output_dir)
    finally:
        shutil.rmtree(temp_dir)
    cex_generated = len(os.listdir(output_dir))
    msg = 'Generated ' + str(cex_generated) + ' executions.'
    logger.info(msg)

    if bug_found:
        logger.error('Found an assertion violation. '
                     'Inspect counterexamples before collecting a '
                     'coverage measure.')
        raise FoundBugException()
    return cex_generated

def parse_coverage_file(coverage_file):

    lines_covered = set()
    # Some lines, such as comments and blank lines, cannot be covered.
    # These lines never show up in coverage files produced by CPAchecker.
    lines_to_cover = set()
    with open(coverage_file) as f:
        for line in f:
            m = re.match(
                r'^DA:(?P<line_number>[^,]*),(?P<visits>.*)$', line)
            if not m:
                continue
            line_number = int(m.group('line_number'))
            lines_to_cover.add(line_number)
            n_visits = int(m.group('visits'))
            if n_visits != 0:
                lines_covered.add(line_number)

    return lines_covered, lines_to_cover

def get_covered_lines(output_dir):
    coverage_files = counterexample_coverage_files(output_dir)
    assert len(coverage_files) == 1

    lines_covered, _ = parse_coverage_file(coverage_files[0])
    return lines_covered

def get_lines_to_cover(output_dir):
    coverage_file = os.path.join(output_dir, 'coverage.info')
    assert os.path.isfile(coverage_file)

    _, lines_to_cover = parse_coverage_file(coverage_file)
    return lines_to_cover

def get_coverage(cex_spec_file, instance, aa_file, heap_size, logger):
    create_temp_dir(temp_dir)
    specs = [aa_file, cex_spec_file]
    lines_covered = set()
    lines_to_cover = set()
    command = cpachecker_command(
        temp_dir=temp_dir,
        specs=specs,
        heap_size=heap_size,
        cex_count=1,
        timelimit=900,
        instance=instance,
        export_coverage=True)
    try:
        run_command(command, logger)
        lines_covered = get_covered_lines(temp_dir)
        lines_to_cover = get_lines_to_cover(temp_dir)
    finally:
        shutil.rmtree(temp_dir)
    return lines_covered, lines_to_cover

def check_aa(aa_file, logger):
    assert os.path.isfile(aa_file)
    with open(aa_file) as f:
        for line in f:
            if 'ASSUME' in line:
                logger.error(
                    'There are known bugs in ASSUME statements that can '
                    'result in misleading output. '
                    'To generate the Assumption Automaton without ASSUME '
                    'statements, use the following option:\n'
                    'assumptions.automatonIgnoreAssumptions=true')
                raise ValueError(
                    'Assumption Automaton contains ASSUME statement.')

def collect_coverage(instance, aa_file, specs_generator, heap_size, logger):
    check_aa(aa_file, logger)

    lines_covered = set()
    lines_to_cover = set()
    for num, cex in enumerate(specs_generator, start=1):
        new_covered, new_to_cover = get_coverage(
            cex, instance, aa_file, heap_size, logger)
        lines_covered.update(new_covered)
        lines_to_cover.update(new_to_cover)
        logger.info(
            'Coverage after collecting ' + str(num) + ' executions:' )
        logger.info('Lines covered: ' + str(len(lines_covered)))
        logger.info('Total lines to cover: ' + str(len(lines_to_cover)))
        logger.info('')
    logger.info('Total lines covered: ' + str(len(lines_covered)))
    logger.info('Total lines to cover: ' + str(len(lines_to_cover)))
    return lines_covered, lines_to_cover

def create_arg_parser():
    parser = argparse.ArgumentParser()
    
    parser.add_argument(
        "-assumption_automaton_file",
        required=True,
        help="""some_path/assumption_automaton
             File containing an assumption automaton.""")
    parser.add_argument(
        "-cex_dir",
        help=("Directory where traces sampling the execution space are "
              "located. If the option -only_collect_coverage is not "
              "present, then this directory must not exist, since it will "
              "be created and used to store the executions."))
    parser.add_argument(
        "-cex_count",
        type=int,
        help="Only applicable (and required) when -only_collect_coverage "
        "is not present. Indicates the number of traces to be generated.""")
    parser.add_argument(
        "-only_collect_coverage",
        action='store_true',
        help="Do not generate traces before collecting coverage.")
    parser.add_argument(
        "-debug",
        action='store_true',
        help="Verbose output.")
    parser.add_argument(
        "-timelimit",
        type=int,
        help=("Only applicable when -only_collect_coverage is not present.\n"
              "Time limit in seconds: We sample the execution space by "
              "repeatedly calling CPAchecker, this would be a global time limit "
              "across several calls."))
    parser.add_argument(
        "-spec",
        required=True,
        help=("Only applicable when -only_collect_coverage is not present.\n"
              "CPAchecker specification file: We sample the execution space by "
              "repeatedly calling CPAchecker, if a specification violation was "
              "found, we will produce an error message for the executions "
              "generated to be manually inspected."))

    parser.add_argument(
        "-heap", help="Heap size limit to be used by CPAchecker.")
    parser.add_argument(
        "instance_filename", help="Instance filename.")
    return parser

def check_args(args, logger):
    check_aa(args.assumption_automaton_file, logger)

    if (args.cex_count or args.timelimit) and (args.only_collect_coverage):
        logger.error((
            'Invalid options: Options -cex_count can only be '
            'present when -only_collect_coverage is not present.'))
        sys.exit(0)
    if not args.only_collect_coverage:
        if not args.cex_count:
            logger.error((
                'Option -cex_count is required with -only_collect_coverage '
                'is not present.'))
            sys.exit(0)
        if os.path.exists(args.cex_dir):
            logger.error((
                'Invalid option: when not using -only_collect_coverage, the '
                'directory -cex_dir (' + args.cex_dir + ') must not '
                'exist. The directory will be created by this script '
                'and will contain the generated executions.'))
            sys.exit(0)
        if not os.path.isfile(args.spec):
            logger.error(
                'Invalid option: Specification file does not exist: ' +
                args.spec)
            sys.exit(0)
    elif (args.cex_count or args.timelimit):
        logger.error(
            ('Invalid options: Options -cex_count and -timelimit can only '
             'be present when -only_collect_coverage is not present.'))
        sys.exit(0)

def gen_specs_from_dir(cex_dir):
    for spec in counterexample_spec_files(cex_dir):
        yield spec

def main(argv, logger):
    parser = create_arg_parser()
    if len(argv)==0:
        parser.print_help()
        sys.exit(1)
    args = parser.parse_args(argv)
    if args.debug:
        logger.setLevel(logging.DEBUG)
    check_args(args, logger)
    
    if not args.only_collect_coverage:
        generate_executions(
            instance=args.instance_filename,
            output_dir=args.cex_dir,
            cex_count=args.cex_count,
            spec=args.spec,
            heap_size=args.heap,
            timelimit=args.timelimit,
            logger=logger)
    collect_coverage(
        instance=args.instance_filename,
        aa_file=args.assumption_automaton_file,
        specs_generator=gen_specs_from_dir(args.cex_dir),
        heap_size=args.heap,
        logger=logger)
