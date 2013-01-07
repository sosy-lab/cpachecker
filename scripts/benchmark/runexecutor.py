"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2012  Dirk Beyer
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

import logging
import os
import resource
import signal
import subprocess
import threading
import time

import benchmark.util as Util

MEMLIMIT = "memlimit"
TIMELIMIT = "timelimit"

_BYTE_FACTOR = 1024 # byte in kilobyte

_SUB_PROCESSES = set()
_SUB_PROCESSES_LOCK = threading.Lock()

def executeRun(args, rlimits, outputFileName, cpuIndex=None, memdata=False):
    """
    This function executes a given command with resource limits,
    and writes the output to a file.
    @param args: the command line to run
    @param rlimits: the resource limits
    @param outputFileName: the file where the output should be written to
    @param cpuIndex: None or the number of a cpu core to use
    @param memdata: whether RLIMIT_DATA should be used instead of RLIMIT_AS for memory limit
    @return: a tuple with wallTime, cpuTime, returnvalue, and process output
    """
    def preSubprocess():
        os.setpgrp() # make subprocess to group-leader

        if TIMELIMIT in rlimits:
            resource.setrlimit(resource.RLIMIT_CPU, (rlimits[TIMELIMIT], rlimits[TIMELIMIT]))
        if MEMLIMIT in rlimits:
            memresource = resource.RLIMIT_DATA if memdata else resource.RLIMIT_AS
            memlimit = rlimits[MEMLIMIT] * _BYTE_FACTOR * _BYTE_FACTOR # MB to Byte
            resource.setrlimit(memresource, (memlimit, memlimit))


    if cpuIndex:
        # use only one cpu for one subprocess
        import multiprocessing
        args = ['taskset', '-c', str(cpuIndex % multiprocessing.cpu_count())] + args

    outputFile = open(outputFileName, 'w') # override existing file
    outputFile.write(' '.join(args) + '\n\n\n' + '-'*80 + '\n\n\n')
    outputFile.flush()

    logging.debug("Executing {0}.".format(args))

    registeredSubprocess = False
    timer = None
    wallTimeBefore = time.time()

    try:
        p = subprocess.Popen(args,
                             stdout=outputFile, stderr=outputFile,
                             preexec_fn=preSubprocess)

        try:
            _SUB_PROCESSES_LOCK.acquire()
            _SUB_PROCESSES.add(p)
            registeredSubProcess = True
        finally:
            _SUB_PROCESSES_LOCK.release()

        # if rlimit does not work, a separate Timer is started to kill the subprocess,
        # Timer has 10 seconds 'overhead'
        if TIMELIMIT in rlimits:
          timelimit = rlimits[TIMELIMIT]
          timer = threading.Timer(timelimit + 10, _killSubprocess, [p])
          timer.start()

        (pid, returnvalue, ru_child) = os.wait4(p.pid, 0)
    except OSError:
        logging.critical("I caught an OSError. Assure that the directory "
                         + "containing the tool to be benchmarked is included "
                         + "in the PATH environment variable or an alias is set.")
        sys.exit("A critical exception caused me to exit non-gracefully. Bye.")

    finally:
        if registeredSubprocess:
            try:
                _SUB_PROCESSES_LOCK.acquire()
                assert p in _SUB_PROCESSES
                _SUB_PROCESSES.remove(p)
            finally:
                _SUB_PROCESSES_LOCK.release()

        if timer and timer.isAlive():
            timer.cancel()

        outputFile.close() # normally subprocess closes file, we do this again

    wallTimeAfter = time.time()
    wallTime = wallTimeAfter - wallTimeBefore
    cpuTime = (ru_child.ru_utime + ru_child.ru_stime)

    assert pid == p.pid

    outputFile = open(outputFileName, 'r') # re-open file for reading output
    output = map(Util.decodeToString, outputFile.readlines()[6:]) # first 6 lines are for logging, rest is output of subprocess
    outputFile.close()

    # Segmentation faults and some memory failures reference a file with more information.
    # We append this file to the log.
    next = False
    for line in output:
        if next:
            try:
                dumpFile = line.strip(' #')
                Util.appendFileToFile(dumpFile, outputFileName)
                os.remove(dumpFile)
            except IOError as e:
                logging.warn('Could not append additional segmentation fault information (%s)' % e.strerror)
            break
        if line == '# An error report file with more information is saved as:':
            next = True

    return (wallTime, cpuTime, returnvalue, '\n'.join(output))

def killAllProcesses():
    try:
        _SUB_PROCESSES_LOCK.acquire()
        for process in _SUB_PROCESSES:
            _killSubprocess(process)
    finally:
        _SUB_PROCESSES_LOCK.release()


def _killSubprocess(process):
    '''
    this function kills the process and the children in its group.
    '''
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except OSError: # process itself returned and exited before killing
        pass