# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import collections
import sys

sys.dont_write_bytecode = True  # prevent creation of .pyc files

import json
import logging
import os
import shutil
import threading

from requests import HTTPError
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import as_completed

import benchexec
import benchexec.tooladapter
from . import util
from .webclient import (
    WebInterface,
    WebClientError,
    UserAbortError,
    handle_result,
    RESULT_FILE_STDERR,
)

"""
This module provides a BenchExec integration for the web interface of the VerifierCloud.
"""

_webclient = None
_print_lock = threading.Lock()


def init(config, benchmark):
    global _webclient

    if not config.debug:
        logging.getLogger("requests").setLevel(logging.WARNING)

    if not benchmark.config.cpu_model:
        logging.warning(
            "It is strongly recommended to set a CPU model('--cloudCPUModel'). "
            "Otherwise the used machines and CPU models are undefined."
        )

    if not config.cloudMaster:
        logging.warning("No URL of a VerifierCloud instance is given.")
        return

    if not config.revision:
        config.revision = "trunk:HEAD"

    _webclient = WebInterface(
        config.cloudMaster,
        config.cloudUser,
        config.revision,
        config.cloud_threads,
        config.cloud_poll_interval,
        user_agent="BenchExec",
        version=benchexec.__version__,
    )

    benchmark.tool_version = _webclient.tool_revision()
    benchmark.executable = "scripts/cpa.sh"
    logging.info("Using %s version %s.", benchmark.tool_name, benchmark.tool_version)


def get_system_info():
    return None


def execute_benchmark(benchmark, output_handler):
    global _webclient

    if benchmark.tool_name != _webclient.tool_name():
        logging.warning("The web client does only support %s.", _webclient.tool_name())
        return 1

    if not _webclient:
        logging.warning("No valid URL of a VerifierCloud instance is given.")
        return 1

    STOPPED_BY_INTERRUPT = False
    try:
        for runSet in benchmark.run_sets:
            if STOPPED_BY_INTERRUPT:
                break

            if not runSet.should_be_executed():
                output_handler.output_for_skipping_run_set(runSet)
                continue

            output_handler.output_before_run_set(runSet)
            try:
                result_futures = _submitRunsParallel(runSet, benchmark, output_handler)
                _handle_results(result_futures, output_handler, benchmark, runSet)
            except (KeyboardInterrupt, UserAbortError):
                STOPPED_BY_INTERRUPT = True
                output_handler.set_error("interrupted", runSet)
            output_handler.output_after_run_set(runSet)
    except WebClientError as e:
        logging.error("%s", e)
        return 1
    finally:
        stop()
        output_handler.output_after_benchmark(STOPPED_BY_INTERRUPT)


def stop():
    global _webclient
    if _webclient:
        _webclient.shutdown()
        _webclient = None


def _submitRunsParallel(runSet, benchmark, output_handler):
    global _webclient

    logging.info("Submitting runs...")

    meta_information = json.dumps(
        {
            "tool": {
                "name": _webclient.tool_name(),
                "revision": _webclient.tool_revision(),
                "benchexec-module": benchmark.tool_module,
            },
            "benchmark": benchmark.name,
            "timestamp": benchmark.instance,
            "runSet": runSet.real_name or "",
            "generator": "benchmark.webclient_benchexec.py",
        }
    )

    executor = ThreadPoolExecutor(max_workers=_webclient.thread_count)
    submission_futures = {}
    submissonCounter = 1
    limits = benchmark.rlimits
    if limits.cpu_cores and limits.cpu_cores != benchmark.requirements.cpu_cores:
        logging.warning("CPU core requirement is not supported by the WebInterface.")
    if limits.memory and limits.memory != benchmark.requirements.memory:
        logging.warning("Memory requirement is not supported by the WebInterface.")
    limits = benchexec.tooladapter.convert_resource_limits_to_dict(limits)

    global_required_files = set(benchmark._required_files)
    cpu_model = benchmark.requirements.cpu_model
    priority = benchmark.config.cloudPriority
    result_files_patterns = benchmark.result_files_patterns
    if not result_files_patterns:
        logging.warning(
            "No result files pattern is given and the result will not contain any result files."
        )

    for run in runSet.runs:
        required_files = global_required_files.union(run.required_files)
        submisson_future = executor.submit(
            _webclient.submit,
            run=run,
            limits=limits,
            cpu_model=cpu_model,
            required_files=required_files,
            meta_information=meta_information,
            priority=priority,
            result_files_patterns=result_files_patterns,
        )
        submission_futures[submisson_future] = run

    executor.shutdown(wait=False)

    # collect results futures
    result_futures = {}
    try:
        for future in as_completed(submission_futures.keys()):
            try:
                run = submission_futures[future]
                result_futures[future.result()] = run

                if submissonCounter % 50 == 0:
                    logging.info(
                        "Submitted run %s/%s", submissonCounter, len(runSet.runs)
                    )

            except HTTPError as e:
                output_handler.set_error("VerifierCloud problem", runSet)
                body = getattr(e.request, "body", None)
                if body:
                    raise WebClientError(
                        'Could not submit run {}, got error "{}" for request with body "{}"'.format(
                            run.identifier, e, body[:200]
                        )
                    )
                else:
                    raise WebClientError(
                        'Could not submit run {}, got error "{}"'.format(
                            run.identifier, e
                        )
                    )

            finally:
                submissonCounter += 1
    finally:
        for future in submission_futures.keys():
            future.cancel()  # for example in case of interrupt

    threadlocal_webclient = _webclient
    if threadlocal_webclient:
        threadlocal_webclient.flush_runs()
    logging.info("Run submission finished.")
    return result_futures


def _log_future_exception(result):
    if result.exception() is not None:
        logging.warning("Error during result processing.", exc_info=True)


def _handle_results(result_futures, output_handler, benchmark, run_set):
    if not _webclient:
        raise UserAbortError("User interrupt detected during _handle_results")
    executor = ThreadPoolExecutor(max_workers=_webclient.thread_count)

    for result_future in as_completed(result_futures.keys()):
        run = result_futures[result_future]
        try:
            result = result_future.result()
            f = executor.submit(
                _unzip_and_handle_result, result, run, output_handler, benchmark
            )
            f.add_done_callback(_log_future_exception)

        except WebClientError as e:
            output_handler.set_error("VerifierCloud problem", run_set)
            logging.warning("Execution of %s failed: %s", run.identifier, e)
        except UserAbortError as e:
            output_handler.set_error("interrupted", run_set)
            logging.warning("Execution of %s aborted: %s", run.identifier, e)

    executor.shutdown(wait=True)


def _unzip_and_handle_result(zip_content, run, output_handler, benchmark):
    """
    Call handle_result with appropriate parameters to fit into the BenchExec expectations.
    """
    result_values = collections.OrderedDict()

    def _open_output_log(output_path):
        log_file = open(run.log_file, "wb")
        log_header = (
            " ".join(run.cmdline())
            + "\n\n\n--------------------------------------------------------------------------------\n"
        )
        log_file.write(log_header.encode("utf-8"))
        return log_file

    def _handle_run_info(values):
        result_values.update(util.parse_vcloud_run_result(values.items()))

    def _handle_host_info(values):
        host = values.pop("name", "-")
        memory = values.pop("memory", None)
        if memory.endswith("byte"):
            memory = int(memory[:-4].strip())
        elif memory:
            memory = benchexec.util.parse_memory_value(memory)
        turboboost_supported = values.pop("turboboost-supported", None) == "true"
        turboboost_enabled = values.pop("turboboost-enabled", None) == "true"
        output_handler.store_system_info(
            values.pop("os", "-"),
            values.pop("cpuModel", "-"),
            values.pop("cores", "-"),
            util.parse_frequency_value(values.pop("frequency", None)) or "-",
            memory or "-",
            host,
            runSet=run.runSet,
            cpu_turboboost=turboboost_enabled if turboboost_supported else None,
        )

        for key, value in values.items():
            result_values["vcloud-" + key] = value

        result_values["host"] = host

    def _handle_stderr_file(result_zip_file, files, output_path):
        if RESULT_FILE_STDERR in files:
            log_dir = os.path.dirname(run.log_file)
            result_zip_file.extract(RESULT_FILE_STDERR, log_dir)
            shutil.move(
                os.path.join(log_dir, RESULT_FILE_STDERR), run.log_file + ".stdError"
            )

    handle_result(
        zip_content,
        run.result_files_folder,
        run.identifier,
        result_files_patterns=benchmark.result_files_patterns,
        open_output_log=_open_output_log,
        handle_run_info=_handle_run_info,
        handle_host_info=_handle_host_info,
        handle_special_files=_handle_stderr_file,
    )

    if result_values:
        with _print_lock:
            output_handler.output_before_run(run)
            run.set_result(result_values, ["host"])
            output_handler.output_after_run(run)
