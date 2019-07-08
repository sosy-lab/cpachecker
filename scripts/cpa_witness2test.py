#!/usr/bin/env python3

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2018  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

import sys
import os
import glob
import subprocess
import shutil
import argparse

import logging
import re

"""
CPA-witness2test module for validating witness files by using a generate-and-validate approach.
Creates a test harness based on the violation witness given for an input file,
compiles the file with the created harness and checks whether the created program
reaches the target location specified by the violation witness.

Currently, reachability, overflow and memory safety properties are supported.
"""

__version__ = "0.1"


COMPILE_ARGS_FIXED = ["-D__alias__(x)="]
"""List of compiler arguments that are always passed to the compiler."""

# Strings used to match expected error messages
EXPECTED_ERRMSG_REACH = "cpa_witness2test: violation"
EXPECTED_ERRMSG_OVERFLOW = "runtime error:"
EXPECTED_ERRMSG_MEM_FREE = "ERROR: AddressSanitizer: attempting free"
EXPECTED_ERRMSG_MEM_DEREF = "ERROR: AddressSanitizer:"
EXPECTED_ERRMSG_MEM_MEMTRACK = "ERROR: AddressSanitizer:"

# Used machine models
MACHINE_MODEL_32 = "32bit"
MACHINE_MODEL_64 = "64bit"

# Possible results of CPAchecker for C harness generation
RESULT_ACCEPT = "FALSE"
RESULT_REJECT = "TRUE"
RESULT_UNK = "UNKNOWN"

# Regular expressions used to match given specification properties
REGEX_REACH = re.compile("G\s*!\s*call\(\s*__VERIFIER_error\(\)\s*\)")
REGEX_OVERFLOW = re.compile("G\s*!\s*overflow")
_REGEX_MEM_TEMPLATE = "G\s*valid-%s"
REGEX_MEM_FREE = re.compile(_REGEX_MEM_TEMPLATE % "free")
REGEX_MEM_DEREF = re.compile(_REGEX_MEM_TEMPLATE % "deref")
REGEX_MEM_MEMTRACK = re.compile(_REGEX_MEM_TEMPLATE % "memtrack")

# Names of supported specifications
SPEC_REACH = "unreach-call"
SPEC_OVERFLOW = "no-overflow"
SPEC_MEM_FREE = "valid-free"
SPEC_MEM_DEREF = "valid-deref"
SPEC_MEM_MEMTRACK = "valid-memtrack"
# specifications -> regular expressions
SPECIFICATIONS = {
    SPEC_REACH: REGEX_REACH,
    SPEC_OVERFLOW: REGEX_OVERFLOW,
    SPEC_MEM_FREE: REGEX_MEM_FREE,
    SPEC_MEM_DEREF: REGEX_MEM_DEREF,
    SPEC_MEM_MEMTRACK: REGEX_MEM_MEMTRACK,
}


class ValidationError(Exception):
    """Exception representing a validation error."""

    def __init__(self, msg):
        self._msg = msg

    @property
    def msg(self):
        return self._msg


class ExecutionResult(object):
    """Results of a subprocess execution."""

    def __init__(self, returncode, stdout, stderr):
        """Create a new ExecutionResult with the given information about
        the execution.

        :param int returncode: Return code of the execution.
        :param Optional[str] stdout: Output that the execution wrote to stdout,
                if any.
        :param Optionl[str] stderr: Output that the execution wrote to stderr,
                if any.
        """
        self._returncode = returncode
        self._stdout = stdout
        self._stderr = stderr

    @property
    def returncode(self):
        return self._returncode

    @property
    def stdout(self):
        return self._stdout

    @property
    def stderr(self):
        return self._stderr


def get_cpachecker_version():
    """Return the CPAchecker version used."""

    executable = get_cpachecker_executable()
    result = execute([executable, "-help"], quiet=True)
    for line in result.stdout.split(os.linesep):
        if line.startswith("CPAchecker"):
            return line.replace("CPAchecker", "").strip()
    return None


def create_parser():
    descr = "Validate a given violation witness for an input file."
    if sys.version_info >= (3, 5):
        parser = argparse.ArgumentParser(
            description=descr, add_help=False, allow_abbrev=False
        )
    else:
        parser = argparse.ArgumentParser(description=descr, add_help=False)

    parser.add_argument("-help", action="help")

    parser.add_argument(
        "-version", action="version", version="{}".format(get_cpachecker_version())
    )

    machine_model_args = parser.add_mutually_exclusive_group(required=False)
    machine_model_args.add_argument(
        "-32",
        dest="machine_model",
        action="store_const",
        const=MACHINE_MODEL_32,
        help="use 32 bit machine model",
    )
    machine_model_args.add_argument(
        "-64",
        dest="machine_model",
        action="store_const",
        const=MACHINE_MODEL_64,
        help="use 64 bit machine model",
    )
    machine_model_args.set_defaults(machine_model=MACHINE_MODEL_32)

    parser.add_argument(
        "-outputpath",
        dest="output_path",
        type=str,
        action="store",
        default="output",
        help="path where output should be stored",
    )

    parser.add_argument("-stats", action="store_true", help="show statistics")

    parser.add_argument(
        "-gcc-args",
        dest="compile_args",
        type=str,
        action="store",
        nargs=argparse.REMAINDER,
        default=[],
        help="list of arguments to use when compiling the counterexample test",
    )

    parser.add_argument(
        "-spec",
        dest="specification_file",
        type=str,
        action="store",
        help="specification file",
    )

    parser.add_argument(
        "-witness", dest="witness_file", type=str, action="store", help="witness file"
    )

    parser.add_argument(
        "file", type=str, nargs="?", help="file to validate witness for"
    )

    return parser


def _parse_args(argv=sys.argv[1:]):
    parser = create_parser()
    args = parser.parse_known_args(argv[:-1])[0]
    args_file = parser.parse_args([argv[-1]])  # Parse the file name
    args.file = args_file.file

    return args


def _create_compile_basic_args(args):
    compile_args = COMPILE_ARGS_FIXED + [x for x in args.compile_args if x is not None]
    if args.machine_model == MACHINE_MODEL_64:
        compile_args.append("-m64")
    elif args.machine_model == MACHINE_MODEL_32:
        compile_args.append("-m32")
    else:
        raise ValidationError("Neither 32 nor 64 bit machine model specified")

    return compile_args


def _create_compiler_cmd_tail(harness, file, target):
    return ["-o", target, "-include", file, harness]


def create_compile_cmd(harness, target, args, specification, c_version="gnu11"):
    """Create the compile command.

    :param str harness: path to harness file
    :param str target: path to program under test
    :param args: arguments as parsed by argparse
    :param list specification: list of properties to check against
    :param str c_version: C standard to use for compilation
    :return: list of command-line keywords that can be given to method `execute`
    """

    if shutil.which("clang"):
        compiler = "clang"
    else:
        compiler = "gcc"

    compile_cmd = [compiler] + _create_compile_basic_args(args)
    compile_cmd.append("-std={}".format(c_version))

    sanitizer_in_use = False
    if SPEC_OVERFLOW in specification:
        sanitizer_in_use = True
        compile_cmd += [
            "-fsanitize=signed-integer-overflow",
            "-fsanitize=float-cast-overflow",
        ]
    if any(
        spec in specification
        for spec in (SPEC_MEM_FREE, SPEC_MEM_DEREF, SPEC_MEM_MEMTRACK)
    ):
        sanitizer_in_use = True
        compile_cmd += ["-fsanitize=address", "-fsanitize=leak"]

    if sanitizer_in_use:
        # Do not continue execution after a sanitize error
        compile_cmd.append("-fno-sanitize-recover")
    compile_cmd += _create_compiler_cmd_tail(harness, args.file, target)
    return compile_cmd


def _create_cpachecker_args(args):
    cpachecker_args = sys.argv[1:]

    for compile_arg in ["-gcc-args"] + args.compile_args:
        if compile_arg in cpachecker_args:
            cpachecker_args.remove(compile_arg)

    cpachecker_args.append("-witness2test")

    return cpachecker_args


def get_cpachecker_executable():
    """Return the path to the CPAchecker executable 'cpa.sh'.
    If the executable is available in the systeme PATH, this executable is
    used. Otherwise, it is checked whether an executable 'cpa.sh' is
    available in the current directory './' or the './scripts' directory.

    :return str: the path to the executable.
    :raise ValidationError: if no CPAchecker executable found.
    """
    executable_name = "cpa.sh"

    def is_exe(exe_path):
        return os.path.isfile(exe_path) and os.access(exe_path, os.X_OK)

    # Directories the CPAchecker executable may ly in.
    # It's important to put '.' and './scripts' last, because we
    # want to look at the "real" PATH directories first
    script_dir = os.path.dirname(os.path.realpath(__file__))
    path_candidates = os.environ["PATH"].split(os.pathsep) + [
        script_dir,
        ".",
        "." + os.sep + "scripts",
    ]
    for path in path_candidates:
        path = path.strip('"')
        exe_file = os.path.join(path, executable_name)
        if is_exe(exe_file):
            return exe_file

    raise ValidationError("CPAchecker executable not found or not executable!")


def create_harness_gen_cmd(args):
    cpa_executable = get_cpachecker_executable()
    harness_gen_args = _create_cpachecker_args(args)
    return [cpa_executable] + harness_gen_args


def find_harnesses(output_path):
    """Returns a list of all harness files found in the given directory."""
    return glob.glob(output_path + "/*harness.c")


def get_target_name(harness_name):
    """Returns a name for the given harness file name."""
    harness_number = re.search(r"(\d+)\.harness\.c", harness_name).group(1)

    return "test_cex" + harness_number


def execute(command, quiet=False):
    """Execute the given command.

    :param List[str] command: list of words that describe the command line.
    :param Bool quiet: whether to log the executed command line as INFO.
    :return ExecutionResult: result object with information about the execution.
    """
    if not quiet:
        logging.info(" ".join(command))
    p = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True
    )
    returncode = p.wait()
    output = p.stdout.read()
    err_output = p.stderr.read()
    return ExecutionResult(returncode, output, err_output)


def analyze_result(test_result, harness, specification):
    """Analyze the given test result and return its verdict.

    :param ExecutionResult test_result: result of test execution
    :param str harness: path to harness file
    :param list specification: list of properties that are part of the specification
    :return: tuple of the verdict of the test execution and the violated property, if any.
        The verdict is one of RESULT_ACCEPT, RESULT_REJECT and RESULT_UNK.
        The violated property is one element of the given specification.
    """
    results_and_violated_props = list()

    def check(code, err_msg, spec_property):
        results_and_violated_props.append(
            _analyze_result_values(test_result, harness, code, err_msg, spec_property)
        )

    # For each specification property, check whether an error message
    # showing its violation was printed
    # TODO: Turn into dict() with loop to be more flexible and remove magic numbers
    if SPEC_REACH in specification:
        check(107, EXPECTED_ERRMSG_REACH, SPEC_REACH)
    if SPEC_OVERFLOW in specification:
        check(1, EXPECTED_ERRMSG_OVERFLOW, SPEC_OVERFLOW)
    if SPEC_MEM_FREE in specification:
        check(1, EXPECTED_ERRMSG_MEM_FREE, SPEC_MEM_FREE)
    if SPEC_MEM_DEREF in specification:
        check(1, EXPECTED_ERRMSG_MEM_DEREF, SPEC_MEM_DEREF)
    if SPEC_MEM_MEMTRACK in specification:
        check(1, EXPECTED_ERRMSG_MEM_MEMTRACK, SPEC_MEM_MEMTRACK)

    results = [r[0] for r in results_and_violated_props]
    if RESULT_ACCEPT in results:
        violated_prop = results_and_violated_props[results.index(RESULT_ACCEPT)][1]
        return RESULT_ACCEPT, violated_prop
    elif RESULT_UNK in results:
        return RESULT_UNK, None
    else:
        return RESULT_REJECT, None


def _analyze_result_values(
    test_result, harness, expected_returncode, expected_errmsg, spec_prop
):
    if (
        test_result.returncode == expected_returncode
        and test_result.stderr
        and expected_errmsg in test_result.stderr
    ):
        logging.info(
            "Harness {} reached expected property violation ({}).".format(
                harness, spec_prop
            )
        )
        return RESULT_ACCEPT, spec_prop
    elif test_result.returncode == 0:
        logging.info("Harness {} did not encounter _any_ error".format(harness))
        return RESULT_REJECT, None
    else:
        logging.info("Run with harness {} was not successful".format(harness))
        return RESULT_UNK, None


def _log_multiline(msg, level=logging.INFO):
    if type(msg) is list:
        msg_lines = msg
    else:
        msg_lines = msg.split("\n")
    for line in msg_lines:
        logging.log(level, line)


def get_spec(specification_file):
    """Return the list of specification properties defined by the given
    specification file.

    :param str specification_file: specification file to read.
    :return List[str]: list of specification properties
    :raise ValidationError: if no specification file given or it doesn't exist
    """

    if not specification_file:
        raise ValidationError("No specification file given.")
    if not os.path.isfile(specification_file):
        raise ValidationError(
            "Specification file does not exist: %s" % specification_file
        )

    with open(specification_file, "r") as inp:
        content = inp.read().strip()

    specification = list()
    spec_matches = re.match(
        "CHECK\(\s*init\(.*\),\s*LTL\(\s*(.+)\s*\)\\r*\\n*", content
    )
    if spec_matches:
        for spec, regex in SPECIFICATIONS.items():
            if regex.search(content):
                specification.append(spec)

    if not specification:
        raise ValidationError("No SV-COMP specification found in " + specification_file)
    return specification


def run():
    statistics = []
    args = _parse_args()
    output_dir = args.output_path

    specification = get_spec(args.specification_file)

    harness_gen_cmd = create_harness_gen_cmd(args)
    harness_gen_result = execute(harness_gen_cmd)
    print(harness_gen_result.stderr)
    _log_multiline(harness_gen_result.stdout, level=logging.DEBUG)

    created_harnesses = find_harnesses(output_dir)
    statistics.append(("Harnesses produced", len(created_harnesses)))

    final_result = None
    violated_property = None
    successful_harness = None
    iter_count = 0  # Count how many harnesses were tested
    compile_success_count = 0  # Count how often compilation overall was successful
    c11_success_count = 0  # Count how often compilation with C11 standard was sucessful
    reject_count = 0
    for harness in created_harnesses:
        iter_count += 1
        logging.info("Looking at {}".format(harness))
        exe_target = output_dir + os.sep + get_target_name(harness)
        compile_cmd = create_compile_cmd(harness, exe_target, args, specification)
        compile_result = execute(compile_cmd)

        _log_multiline(compile_result.stderr, level=logging.INFO)
        _log_multiline(compile_result.stdout, level=logging.DEBUG)

        if compile_result.returncode != 0:
            compile_cmd = create_compile_cmd(
                harness, exe_target, args, specification, "gnu90"
            )
            compile_result = execute(compile_cmd)
            _log_multiline(compile_result.stderr, level=logging.INFO)
            _log_multiline(compile_result.stdout, level=logging.DEBUG)

            if compile_result.returncode != 0:
                logging.warning("Compilation failed for harness {}".format(harness))
                continue

        else:
            c11_success_count += 1
        compile_success_count += 1

        test_result = execute([exe_target])
        test_stdout_file = output_dir + os.sep + "stdout.txt"
        test_stderr_file = output_dir + os.sep + "stderr.txt"
        if test_result.stdout:
            with open(test_stdout_file, "w+") as output:
                output.write(test_result.stdout)
                logging.info(
                    "Wrote stdout of test execution to {}".format(test_stdout_file)
                )
        if test_result.stderr:
            with open(test_stderr_file, "w+") as error_output:
                error_output.write(test_result.stderr)
                logging.info(
                    "Wrote stderr of test execution to {}".format(test_stderr_file)
                )

        result, new_violated_property = analyze_result(
            test_result, harness, specification
        )
        if result == RESULT_ACCEPT:
            successful_harness = harness
            final_result = RESULT_ACCEPT
            if not violated_property:  # Use first violated property
                violated_property = new_violated_property
            break
        elif result == RESULT_REJECT:
            reject_count += 1
            if not final_result:
                # Only set final result to 'reject' if no harness produces any error
                final_result = RESULT_REJECT
        else:
            final_result = RESULT_UNK

    if compile_success_count == 0:
        raise ValidationError("Compilation failed for every harness/file pair.")

    statistics.append(("Harnesses tested", iter_count))
    statistics.append(("C11 compatible", c11_success_count))
    statistics.append(("Harnesses rejected", reject_count))

    if args.stats:
        print(os.linesep + "Statistics:")
        for prop, value in statistics:
            print("\t" + str(prop) + ": " + str(value))
        print()

    if successful_harness:
        print("Harness %s was successful." % successful_harness)

    result_str = "Verification result: %s" % final_result
    if violated_property:
        result_str += (
            ". Property violation (%s) found by chosen configuration."
            % violated_property
        )
    print(result_str)


logging.basicConfig(format="%(levelname)s: %(message)s", level=logging.INFO)

if __name__ == "__main__":
    try:
        run()
    except ValidationError as e:
        logging.error(e.msg)
        print("Verification result: ERROR.")
