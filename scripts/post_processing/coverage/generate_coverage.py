#!/usr/bin/python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2017 Rodrigo Castano
# SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import argparse
import logging
import os
import os.path
import re
import shutil
import subprocess
import sys
import time
from subprocess import check_output


class FoundBugError(Exception):
    pass


script_path = os.path.dirname(os.path.realpath(__file__))
cpachecker_root = os.path.join(script_path, os.pardir, os.pardir, os.pardir)

# Not thread safe
temp_dir = os.path.join(script_path, "temp_dir_coverage")


def print_command(command, logger):
    for c in command[:-1]:
        logger.debug(c + " \\")
    logger.debug(command[-1])


def create_temp_dir(temp_dir):
    shutil.rmtree(temp_dir, ignore_errors=True)
    os.makedirs(temp_dir)


coverage_test_case_message = "Found covering test case"


def gen_reach_exit_spec(f):
    print("CONTROL AUTOMATON CoverageAutomaton", file=f)
    print("", file=f)
    print("INITIAL STATE WaitForExit;", file=f)
    print("", file=f)
    print("STATE USEFIRST WaitForExit:", file=f)
    print('  MATCH EXIT -> ERROR("' + coverage_test_case_message + '");', file=f)
    print("", file=f)
    print("END AUTOMATON", file=f)


def gen_covers_any_line_in_set_then_reach_exit_spec(lines_to_cover, f):
    print("CONTROL AUTOMATON CoverageAutomaton", file=f)
    print("", file=f)
    print("INITIAL STATE LookingForLine;", file=f)
    print("", file=f)
    print("STATE USEFIRST LookingForLine:", file=f)
    print(
        "  CHECK("
        + 'AutomatonAnalysis_AssumptionAutomaton, "state == __FALSE") '
        + "-> STOP;",
        file=f,
    )
    lines = map(str, lines_to_cover)
    print("  COVERS_LINES(" + " ".join(lines) + ") -> GOTO WaitForExit;", file=f)
    print("", file=f)
    print("STATE USEFIRST WaitForExit:", file=f)
    print('  MATCH EXIT -> ERROR("' + coverage_test_case_message + '");', file=f)
    print("", file=f)
    print("END AUTOMATON", file=f)


def create_spec(spec_folder):
    reach_exit_spec_file = os.path.join(spec_folder, "spec" + spec_extension)
    with open(reach_exit_spec_file, "w") as f:
        gen_reach_exit_spec(f)
    return reach_exit_spec_file


def create_spec_for_lines(spec_folder, lines_to_cover):
    reach_exit_spec_file = os.path.join(spec_folder, "spec" + spec_extension)
    with open(reach_exit_spec_file, "w") as f:
        gen_covers_any_line_in_set_then_reach_exit_spec(lines_to_cover, f)
    return reach_exit_spec_file


spec_extension = ".spc"


def counterexample_spec_files(cex_dir):
    pattern = r".*Counterexample.([^.]*)" + spec_extension
    all_files = sorted(os.listdir(cex_dir))
    return [os.path.join(cex_dir, cex) for cex in all_files if re.match(pattern, cex)]


cov_extension = ".aa-prefix.coverage-info"


def counterexample_coverage_files(cex_dir):
    pattern = r".*Counterexample.([^.]*)" + cov_extension
    all_files = sorted(os.listdir(cex_dir))
    return [os.path.join(cex_dir, cex) for cex in all_files if re.match(pattern, cex)]


def move_execution_spec_files(temp_dir, output_dir):
    if os.path.exists(output_dir):
        msg = "Output directory (" + output_dir + ") should not exist."
        raise ValueError(msg)
    os.makedirs(output_dir)
    all_cex_specs = counterexample_spec_files(temp_dir)
    for spec in all_cex_specs:
        bn = os.path.basename(spec)
        shutil.copyfile(src=spec, dst=os.path.join(output_dir, bn))


def move_execution_spec_and_cex_coverage_files(temp_dir, output_dir):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    all_cex_specs = counterexample_spec_files(temp_dir)
    all_cex_cov = counterexample_coverage_files(temp_dir)

    # sanity check, should have a coverage file for each .spc file:
    no_extension_cov = [s.replace(cov_extension, "") for s in all_cex_cov]
    no_extension_spc = [s.replace(spec_extension, "") for s in all_cex_specs]
    assert no_extension_cov == no_extension_spc

    def counterexample_filename(path, i, ext):
        return os.path.join(path, "Counterexample." + str(i) + ext)

    i = 1
    new_specs = []
    for cex in no_extension_spc:
        while True:
            if not os.path.exists(
                counterexample_filename(path=output_dir, i=i, ext=spec_extension)
            ):
                break
            i = i + 1
        new_spec = counterexample_filename(path=output_dir, i=i, ext=spec_extension)
        shutil.copyfile(src=cex + spec_extension, dst=new_spec)
        new_specs.append(new_spec)

        # sanity check
        # if spec file didn't exist, coverage file shouldn't exist either
        assert not os.path.exists(
            counterexample_filename(path=output_dir, i=i, ext=cov_extension)
        )
        shutil.copyfile(
            src=cex + cov_extension,
            dst=counterexample_filename(path=output_dir, i=i, ext=cov_extension),
        )
    return new_specs


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


class FoundUserDefinedPropertyViolation:
    def found_property_violation(self):
        return True

    def found_bug(self):
        return True


class OnlyGeneratedSuccessfulExecutions:
    def found_property_violation(self):
        return True

    def found_bug(self):
        return False


class NoPropertyViolationFound:
    def found_property_violation(self):
        return False

    def found_bug(self):
        raise Exception("This method should not have been called")


def parse_result(output, logger):
    pattern = r"Verification result: [^(]*\((?P<message>[^)]*)\).*"
    result_pattern = r"Verification result: (?P<result>.*)"
    m_result = re.search(pattern=result_pattern, string=str(output))
    m = re.search(pattern=pattern, string=str(output))
    if not m_result:
        logger.error("Failed to parse CPAchecker output.")
        return NoPropertyViolationFound()
    if m_result.group("result").startswith("TRUE") or m_result.group(
        "result"
    ).startswith("UNKNOWN"):
        return NoPropertyViolationFound()
    else:
        if not m:
            logger.error("Failed to parse CPAchecker output.")
            return NoPropertyViolationFound()
        if m.group("message") == coverage_test_case_message:
            return OnlyGeneratedSuccessfulExecutions()
        else:
            return FoundUserDefinedPropertyViolation()


class ComputeCoverage:
    def __init__(
        self,
        instance,
        output_dir,
        cex_count,
        spec,
        heap_size,
        timelimit,
        logger,
        aa_file,
        start_time,
        timer,
    ):
        self.instance = instance
        self.output_dir = output_dir
        self.cex_count = cex_count
        self.spec = spec
        self.heap_size = heap_size
        self.timelimit = timelimit
        self.logger = logger
        check_aa(aa_file, logger)
        self.aa_file = aa_file
        self.lines_covered = set()
        self.lines_to_cover = self.compute_lines_to_cover(self.instance, self.logger)
        self.start_time = start_time
        self.timer = timer

    @staticmethod
    def compute_lines_to_cover(instance, logger):
        create_temp_dir(temp_dir)
        command = [
            os.path.join(cpachecker_root, "scripts", "cpa.sh"),
            # Using this configuration because it seems lightweight
            "-detectRecursion",
            "-outputpath",
            temp_dir,
            instance,
        ]
        try:
            run_command(command, logger)
            lines_to_cover = get_lines_to_cover(temp_dir)
        finally:
            shutil.rmtree(temp_dir)

        return lines_to_cover

    @staticmethod
    def cpachecker_command(
        temp_dir,
        specs,
        heap_size,
        timelimit,
        instance,
        export_coverage=False,
        stop_after_error=False,
        cex_count=0,
    ):
        conf = os.path.join(cpachecker_root, "config", "valueAnalysis.properties")
        coverage_options = [
            "-setprop",
            "counterexample.export.exportCounterexampleCoverage=true",
            "-setprop",
            "cpa.composite.aggregateBasicBlocks=false",
        ]
        stop_after_error_opts = (
            [] if stop_after_error else ["-setprop", "analysis.stopAfterError=false"]
        )
        timelimit_prop = []
        if timelimit is None:
            # No time limit (see doc/ConfigurationOptions.txt)
            timelimit_prop = ["-setprop", "limits.time.cpu=-1ns"]
        else:
            timelimit_prop = ["-setprop", "limits.time.cpu=" + str(timelimit) + "s"]

        return (  # noqa: ECE001
            [
                os.path.join(cpachecker_root, "scripts", "cpa.sh"),
                "-config",
                conf,
                "-outputpath",
                temp_dir,
                "-setprop",
                "specification=" + ",".join(specs),
            ]
            + (["-heap", heap_size] if heap_size else [])
            + (stop_after_error_opts)
            + [
                "-setprop",
                "analysis.counterexampleLimit=" + str(cex_count),
                "-setprop",
                "analysis.traversal.usePostorder=true",
            ]
            + (timelimit_prop)
            + [
                "-setprop",
                "analysis.traversal.order=DFS",
                "-setprop",
                "analysis.traversal.useReversePostorder=false",
                "-setprop",
                "analysis.traversal.useCallstack=false",
            ]
            + (coverage_options if export_coverage else [])
            + [instance]
        )

    def generate_executions(self):
        raise NotImplementedError("Instantiate one of the sub-classes.")

    def get_coverage(self, cex_spec_file, instance, aa_file, heap_size, logger):
        cex_prefix_coverage_file = os.path.splitext(cex_spec_file)[0] + cov_extension
        assert os.path.isfile(cex_prefix_coverage_file)
        lines_covered, _ = parse_coverage_file(cex_prefix_coverage_file)
        return lines_covered

    def collect_coverage(self):
        for num, cex in enumerate(self.generate_executions(), start=1):
            new_covered = self.get_coverage(
                cex, self.instance, self.aa_file, self.heap_size, self.logger
            )
            self.lines_covered.update(new_covered)
            self.logger.info("Coverage after collecting " + str(num) + " executions:")
            self.logger.info("Lines covered: " + str(len(self.lines_covered)))
            self.logger.info("Total lines to cover: " + str(len(self.lines_to_cover)))
            self.logger.info("")
        self.logger.info("Total lines covered: " + str(len(self.lines_covered)))
        self.logger.info("Total lines to cover: " + str(len(self.lines_to_cover)))
        return self.lines_covered, self.lines_to_cover


def gen_specs_from_dir(cex_dir):
    for spec in counterexample_spec_files(cex_dir):
        yield spec


class Timer:
    def time(self):
        return time.time()


# When adding additional generators also update argparse documentation.
available_generators = ["fixpoint", "blind"]


# It will be necessary to refactor this code to support custom configuration
# of the generators.
def create_generator(
    name,
    instance,
    output_dir,
    cex_count,
    spec,
    heap_size,
    timelimit,
    logger,
    aa_file,
    start_time,
    timer,
):
    if name not in available_generators:
        raise Exception("Invalid generator name.")
    if name == "fixpoint":
        return FixPointOnCoveredLines(
            instance=instance,
            output_dir=output_dir,
            cex_count=cex_count,
            spec=spec,
            heap_size=heap_size,
            timelimit=timelimit,
            logger=logger,
            aa_file=aa_file,
            start_time=start_time,
            timer=timer,
        )
    if name == "blind":
        if not cex_count:
            logger.error(
                (
                    "Invalid option: when using '-generator_type blind', "
                    "a limit to the number of counterexamples has to be provided "
                    "using -cex_count."
                )
            )
            sys.exit(0)
        return GenerateFirstThenCollect(
            instance=instance,
            output_dir=output_dir,
            cex_count=cex_count,
            spec=spec,
            heap_size=heap_size,
            timelimit=timelimit,
            logger=logger,
            aa_file=aa_file,
            start_time=start_time,
            timer=timer,
        )
    raise Exception("Missing generator constructor.")


def define_iteration_timelimit_from_global_timelimit(
    start_time, global_timelimit, timer
):
    if global_timelimit:
        assert start_time and global_timelimit
        elapsed_time = timer.time() - start_time
        return str(int(float(global_timelimit) - elapsed_time))
    else:
        return None


class FixPointOnCoveredLines(ComputeCoverage):
    def __init__(
        self,
        instance,
        output_dir,
        cex_count,
        spec,
        heap_size,
        timelimit,
        logger,
        aa_file,
        start_time,
        timer,
    ):
        super().__init__(
            instance=instance,
            output_dir=output_dir,
            cex_count=cex_count,
            spec=spec,
            heap_size=heap_size,
            timelimit=timelimit,
            logger=logger,
            aa_file=aa_file,
            start_time=start_time,
            timer=timer,
        )

    def generate_executions(self):
        last_difference_size = None
        cex_created = 0
        while True:
            remaining_lines = self.lines_to_cover.difference(self.lines_covered)
            difference_size = len(remaining_lines)
            # sanity check
            assert (
                last_difference_size is None or last_difference_size > difference_size
            )
            if not difference_size:
                break

            create_temp_dir(temp_dir)
            cover_line_then_reach_exit_spec_file = create_spec_for_lines(
                spec_folder=temp_dir, lines_to_cover=remaining_lines
            )

            specs = [cover_line_then_reach_exit_spec_file, self.spec, self.aa_file]
            timelimit = define_iteration_timelimit_from_global_timelimit(
                start_time=self.start_time,
                global_timelimit=self.timelimit,
                timer=self.timer,
            )
            if timelimit and float(timelimit) < 5:
                self.logger.debug("Preemptively quitting. Less than 10 seconds left.")
                break
            command = self.cpachecker_command(
                temp_dir=temp_dir,
                specs=specs,
                heap_size=self.heap_size,
                timelimit=timelimit,
                instance=self.instance,
                export_coverage=True,
                stop_after_error=True,
            )

            try:
                output = run_command(command, self.logger)
                specs_generated = move_execution_spec_and_cex_coverage_files(
                    temp_dir=temp_dir, output_dir=self.output_dir
                )
            finally:
                shutil.rmtree(temp_dir)

            msg = "Generated " + str(len(specs_generated)) + " executions."
            self.logger.info(msg)

            cpachecker_result = parse_result(output, self.logger)
            # sanity check
            assert (
                cpachecker_result.found_property_violation()
                or len(specs_generated) == 0
            )
            if not cpachecker_result.found_property_violation():
                self.logger.debug("CPAchecker did not generate an execution.")
                break
            if cpachecker_result.found_bug():
                self.logger.error(
                    "Found an assertion violation. Inspect counterexamples "
                    "before collecting a coverage measure."
                )
                raise FoundBugError()
            for spec in specs_generated:
                yield spec
                # we might be ignoring already produced counterexamples
                # if we generate more than once at a time (this is not
                # the case yet)
                cex_created += 1
                if self.cex_count and cex_created >= self.cex_count:
                    break
            # Also need to leave the main loop
            if self.cex_count and cex_created >= self.cex_count:
                break


class GenerateFirstThenCollect(ComputeCoverage):
    def __init__(
        self,
        instance,
        output_dir,
        cex_count,
        spec,
        heap_size,
        timelimit,
        logger,
        aa_file,
        start_time,
        timer,
    ):
        super().__init__(
            instance=instance,
            output_dir=output_dir,
            cex_count=cex_count,
            spec=spec,
            heap_size=heap_size,
            timelimit=timelimit,
            logger=logger,
            aa_file=aa_file,
            start_time=start_time,
            timer=timer,
        )

    def get_coverage(self, cex_spec_file, instance, aa_file, heap_size, logger):
        create_temp_dir(temp_dir)
        specs = [aa_file, cex_spec_file]
        command = self.cpachecker_command(
            temp_dir=temp_dir,
            specs=specs,
            heap_size=heap_size,
            timelimit=900,
            instance=instance,
            export_coverage=True,
            stop_after_error=True,
        )
        try:
            run_command(command, logger)
            lines_covered = get_covered_lines(temp_dir)
        finally:
            shutil.rmtree(temp_dir)
        return lines_covered

    def generate_executions(self):
        create_temp_dir(temp_dir)
        reach_exit_spec_file = create_spec(spec_folder=temp_dir)

        specs = [reach_exit_spec_file, self.spec]
        timelimit = define_iteration_timelimit_from_global_timelimit(
            start_time=self.start_time,
            global_timelimit=self.timelimit,
            timer=self.timer,
        )
        if timelimit and float(timelimit) < 0:
            # using alternative time limit of 10s, this should not be used
            # under normal circumstances
            timelimit = 10
        command = self.cpachecker_command(
            temp_dir=temp_dir,
            specs=specs,
            heap_size=self.heap_size,
            cex_count=self.cex_count,
            timelimit=timelimit,
            instance=self.instance,
        )

        try:
            output = run_command(command, self.logger)
            move_execution_spec_files(temp_dir=temp_dir, output_dir=self.output_dir)
        finally:
            shutil.rmtree(temp_dir)
        cex_generated = len(os.listdir(self.output_dir))
        msg = "Generated " + str(cex_generated) + " executions."
        self.logger.info(msg)

        cpachecker_result = parse_result(output, self.logger)
        # sanity check
        assert cpachecker_result.found_property_violation() or cex_generated == 0
        if (
            cpachecker_result.found_property_violation()
            and cpachecker_result.found_bug()
        ):
            self.logger.error(
                "Found an assertion violation. Inspect counterexamples "
                "before collecting a coverage measure."
            )
            raise FoundBugError()
        return gen_specs_from_dir(self.output_dir)


class CollectFromExistingExecutions(GenerateFirstThenCollect):
    def __init__(
        self,
        instance,
        cex_dir,
        heap_size,
        timelimit,
        logger,
        aa_file,
        start_time,
        timer,
    ):
        super().__init__(
            instance=instance,
            output_dir=cex_dir,
            cex_count=None,
            spec=None,
            heap_size=heap_size,
            timelimit=timelimit,
            logger=logger,
            aa_file=aa_file,
            start_time=start_time,
            timer=timer,
        )

    def generate_executions(self):
        return gen_specs_from_dir(self.output_dir)


def parse_coverage_file(coverage_file):
    lines_covered = set()
    # Some lines, such as comments and blank lines, cannot be covered.
    # These lines never show up in coverage files produced by CPAchecker.
    lines_to_cover = set()
    # This script only supports a single source file right now.
    # For the current use case we don't need more than that but the file
    # format does not seem to complex.
    sf_lines = []
    with open(coverage_file) as f:
        for line in f:
            m_sf = re.match(r"^SF:(?P<sourcefile>.*)$", line)
            if m_sf:
                sf_lines.append(m_sf.group("sourcefile"))
            m = re.match(r"^DA:(?P<line_number>[^,]*),(?P<visits>.*)$", line)
            if not m:
                continue
            line_number = int(m.group("line_number"))
            lines_to_cover.add(line_number)
            n_visits = int(m.group("visits"))
            if n_visits != 0:
                lines_covered.add(line_number)
    assert len(sf_lines) == 1
    # The coverage files produced for counterexample do not contain
    # all the existing lines. Should not output this information.
    if coverage_file.endswith(cov_extension):
        lines_to_cover = None
    return lines_covered, lines_to_cover


def get_covered_lines(output_dir):
    coverage_files = counterexample_coverage_files(output_dir)
    assert len(coverage_files) == 1

    lines_covered, _ = parse_coverage_file(coverage_files[0])
    return lines_covered


def get_lines_to_cover(output_dir):
    coverage_file = os.path.join(output_dir, "coverage.info")
    assert os.path.isfile(coverage_file)

    _, lines_to_cover = parse_coverage_file(coverage_file)
    return lines_to_cover


def check_aa(aa_file, logger):
    assert os.path.isfile(aa_file)
    with open(aa_file) as f:
        for line in f:
            if "ASSUME" in line:
                logger.error(
                    "There are known bugs in ASSUME statements that can "
                    "result in misleading output. "
                    "To generate the Assumption Automaton without ASSUME "
                    "statements, use the following option:\n"
                    "assumptions.automatonIgnoreAssumptions=true"
                )
                raise ValueError("Assumption Automaton contains ASSUME statement.")


def create_arg_parser():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "-assumption_automaton_file",
        required=True,
        help="""some_path/assumption_automaton
             File containing an assumption automaton.""",
    )
    parser.add_argument(
        "-cex_dir",
        required=True,
        help=(
            "Directory where traces sampling the execution space are "
            "located. If the option -only_collect_coverage is not "
            "present, then this directory must not exist, since it will "
            "be created and used to store the executions."
        ),
    )
    parser.add_argument(
        "-cex_count",
        type=int,
        help="Only applicable when -only_collect_coverage "
        "is not present. Indicates the number of traces to be generated."
        "",
    )
    parser.add_argument(
        "-only_collect_coverage",
        action="store_true",
        help="Do not generate traces before collecting coverage.",
    )
    parser.add_argument("-debug", action="store_true", help="Verbose output.")
    parser.add_argument(
        "-timelimit",
        type=int,
        help=(
            "Only applicable when -only_collect_coverage is not present.\n"
            "Time limit in seconds: We sample the execution space by "
            "repeatedly calling CPAchecker, this would be a global time limit "
            "across several calls."
        ),
    )
    parser.add_argument(
        "-spec",
        required=True,
        help=(
            "Only applicable when -only_collect_coverage is not present.\n"
            "CPAchecker specification file: We sample the execution space by "
            "repeatedly calling CPAchecker, if a specification violation was "
            "found, we will produce an error message for the executions "
            "generated to be manually inspected."
        ),
    )

    parser.add_argument("-heap", help="Heap size limit to be used by CPAchecker.")
    parser.add_argument(
        "-generator_type",
        choices=available_generators,
        default="fixpoint",
        help=(
            "Type of generator to be used. 'fixpoint' incrementally "
            "creates executions that cover statements not covered by "
            "previously generated executions. The generator stops producing "
            "executions when the number of generated executions reaches "
            "-cex_count or when CPAchecker returns TRUE or UNKNOWN."
        ),
    )
    parser.add_argument("instance_filename", help="Instance filename.")
    return parser


def check_args(args, logger):
    check_aa(args.assumption_automaton_file, logger)

    if (args.cex_count or args.timelimit) and (args.only_collect_coverage):
        logger.error(
            (
                "Invalid options: Options -cex_count can only be "
                "present when -only_collect_coverage is not present."
            )
        )
        sys.exit(0)
    if not args.only_collect_coverage:
        if os.path.exists(args.cex_dir):
            logger.error(
                (
                    "Invalid option: when not using -only_collect_coverage, the "
                    "directory -cex_dir (" + args.cex_dir + ") must not "
                    "exist. The directory will be created by this script "
                    "and will contain the generated executions."
                )
            )
            sys.exit(0)
        if not os.path.isfile(args.spec):
            logger.error(
                "Invalid option: Specification file does not exist: " + args.spec
            )
            sys.exit(0)
    elif args.cex_count or args.timelimit:
        logger.error(
            (
                "Invalid options: Options -cex_count and -timelimit can only "
                "be present when -only_collect_coverage is not present."
            )
        )
        sys.exit(0)


def main(argv, logger, timer=Timer()):  # noqa: B008
    parser = create_arg_parser()
    if len(argv) == 0:
        parser.print_help()
        sys.exit(1)
    args = parser.parse_args(argv)
    if args.debug:
        logger.setLevel(logging.DEBUG)
    check_args(args, logger)

    start_time = time.time()
    if args.only_collect_coverage:
        compute_coverage = CollectFromExistingExecutions(
            instance=args.instance_filename,
            cex_dir=args.cex_dir,
            heap_size=args.heap,
            timelimit=args.timelimit,
            logger=logger,
            aa_file=args.assumption_automaton_file,
            start_time=start_time,
            timer=timer,
        )
    else:
        compute_coverage = create_generator(
            name=args.generator_type,
            instance=args.instance_filename,
            output_dir=args.cex_dir,
            cex_count=args.cex_count,
            spec=args.spec,
            heap_size=args.heap,
            timelimit=args.timelimit,
            logger=logger,
            aa_file=args.assumption_automaton_file,
            start_time=start_time,
            timer=timer,
        )
    compute_coverage.collect_coverage()
