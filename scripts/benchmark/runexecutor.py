"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2014  Dirk Beyer
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

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import logging
import multiprocessing
import os
import resource
import subprocess
import sys
import threading
import time

from .benchmarkDataStructures import MEMLIMIT, TIMELIMIT, CORELIMIT
from . import util as Util
from .cgroups import *
from . import filewriter
from . import oomhandler

readFile = filewriter.readFile
writeFile = filewriter.writeFile

CPUACCT = 'cpuacct'
CPUSET = 'cpuset'
MEMORY = 'memory'

_BYTE_FACTOR = 1000 # byte in kilobyte
_WALLTIME_LIMIT_OVERHEAD = 30 # seconds


class RunExecutor():

    def __init__(self):
        self.PROCESS_KILLED = False
        self.SUB_PROCESSES_LOCK = threading.Lock() # needed, because we kill the process asynchronous
        self.SUB_PROCESSES = set()

        self._initCGroups()


    def _initCGroups(self):
        """
        This function initializes the cgroups for the limitations.
        Please call it before any calls to executeRun(),
        if you want to separate initialization from actual run execution
        (e.g., for better error message handling).
        """
        self.cgroupsParents = {} # contains the roots of all cgroup-subsystems
        self.cpus = [] # list of available CPU cores

        initCgroup(self.cgroupsParents, CPUACCT)
        if not self.cgroupsParents[CPUACCT]:
            logging.warning('Without cpuacct cgroups, cputime measurement and limit might not work correctly if subprocesses are started.')

        initCgroup(self.cgroupsParents, MEMORY)
        if not self.cgroupsParents[MEMORY]:
            logging.warning('Cannot measure and limit memory consumption without memory cgroups.')

        initCgroup(self.cgroupsParents, CPUSET)

        cgroupCpuset = self.cgroupsParents[CPUSET]
        if not cgroupCpuset:
            logging.warning("Cannot limit the number of CPU cores without cpuset cgroup.")
        else:
            # Read available cpus:
            cpuStr = readFile(cgroupCpuset, 'cpuset.cpus')
            for cpu in cpuStr.split(','):
                cpu = cpu.split('-')
                if len(cpu) == 1:
                    self.cpus.append(int(cpu[0]))
                elif len(cpu) == 2:
                    start, end = cpu
                    self.cpus.extend(range(int(start), int(end)+1))
                else:
                    logging.warning("Could not read available CPU cores from kernel, failed to parse {0}.".format(cpuStr))
    
            logging.debug("List of available CPU cores is {0}.".format(self.cpus))


    def _setupCGroups(self, args, rlimits, myCpuIndex=None):
        """
        This method creates the CGroups for the following execution.
        @param args: the command line to run, used only for logging
        @param rlimits: the resource limits, used for the cgroups
        @param myCpuIndex: number of a CPU-core for the execution, does not match physical cores
        @return cgroups: a map of all the necessary cgroups for the following execution.
                         Please add the process of the following execution to all those cgroups!
        @return myCpuCount: None or the number of CPU cores to use
        """
      
        # Setup cgroups, need a single call to createCgroup() for all subsystems
        subsystems = [CPUACCT, MEMORY]
        if CORELIMIT in rlimits and myCpuIndex is not None:
            subsystems.append(CPUSET)
        cgroups = createCgroup(self.cgroupsParents, *subsystems)

        logging.debug("Executing {0} in cgroups {1}.".format(args, cgroups.values()))

        try:
            myCpuCount = multiprocessing.cpu_count()
        except NotImplementedError:
            myCpuCount = 1

        # Setup cpuset cgroup if necessary to limit the CPU cores to be used.
        if CORELIMIT in rlimits and myCpuIndex is not None:
            myCpuCount = rlimits[CORELIMIT]
  
            if not self.cpus or CPUSET not in cgroups:
                sys.exit("Cannot limit number of CPU cores because cgroups are not available.")
            if myCpuCount > len(self.cpus):
                sys.exit("Cannot execute runs on {0} CPU cores, only {1} are available.".format(myCpuCount, len(self.cpus)))
            if myCpuCount <= 0:
                sys.exit("Invalid number of CPU cores to use: {0}".format(myCpuCount))

            cgroupCpuset = cgroups[CPUSET]
            totalCpuCount = len(self.cpus)
            myCpusStart = (myCpuIndex * myCpuCount) % totalCpuCount
            myCpusEnd = (myCpusStart + myCpuCount - 1) % totalCpuCount
            myCpus = ','.join(map(str, map(lambda i: self.cpus[i], range(myCpusStart, myCpusEnd + 1))))
            writeFile(myCpus, cgroupCpuset, 'cpuset.cpus')
            myCpus = readFile(cgroupCpuset, 'cpuset.cpus')
            logging.debug('Executing {0} with cpu cores [{1}].'.format(args, myCpus))

        # Setup memory limit
        if MEMLIMIT in rlimits:

            if not MEMORY in cgroups:
                sys.exit("Memory limit specified, but cannot be implemented without cgroup support.")

            cgroupMemory = cgroups[MEMORY]
            memlimit = str(rlimits[MEMLIMIT] * _BYTE_FACTOR * _BYTE_FACTOR) # MB to Byte

            limitFile = 'memory.limit_in_bytes'
            writeFile(memlimit, cgroupMemory, limitFile)

            swapLimitFile = 'memory.memsw.limit_in_bytes'
            # We need swap limit because otherwise the kernel just starts swapping
            # out our process if the limit is reached.
            # Some kernels might not have this feature,
            # which is ok if there is actually no swap.
            if not os.path.exists(os.path.join(cgroupMemory, swapLimitFile)):
                if _hasSwap():
                    sys.exit('Kernel misses feature for accounting swap memory (memory.memsw.limit_in_bytes file does not exist in memory cgroup), but machine has swap.')
            else:
                try:
                    writeFile(memlimit, cgroupMemory, swapLimitFile)
                except IOError as e:
                    if e.errno == 95: # kernel responds with error 95 (operation unsupported) if this is disabled
                        sys.exit("Memory limit specified, but kernel does not allow limiting swap memory. Please set swapaccount=1 on your kernel command line.")
                    raise e

            memlimit = readFile(cgroupMemory, limitFile)
            logging.debug('Executing {0} with memory limit {1} bytes.'.format(args, memlimit))

        return (cgroups, myCpuCount)


    def _execute(self, args, rlimits, outputFileName, cgroups, myCpuCount, environments, runningDir):
        """
        This method executes the command line and waits for the termination of it. 
        """

        def preSubprocess():
            os.setpgrp() # make subprocess to group-leader

            if TIMELIMIT in rlimits:
                # Also use ulimit for CPU time limit as a fallback if cgroups are not available
                resource.setrlimit(resource.RLIMIT_CPU, (rlimits[TIMELIMIT], rlimits[TIMELIMIT]))

            # put us into the cgroup(s)
            pid = os.getpid()
            # On some systems, cgrulesngd would move our process into other cgroups.
            # We disable this behavior via libcgroup if available.
            # Unfortunately, logging/printing does not seem to work here.
            from ctypes import cdll
            try:
                libcgroup = cdll.LoadLibrary('libcgroup.so.1')
                failure = libcgroup.cgroup_init()
                if failure:
                    pass
                    #print('Could not initialize libcgroup, error {}'.format(success))
                else:
                    CGROUP_DAEMON_UNCHANGE_CHILDREN = 0x1
                    failure = libcgroup.cgroup_register_unchanged_process(pid, CGROUP_DAEMON_UNCHANGE_CHILDREN)
                    if failure:
                        pass
                        #print('Could not register process to cgrulesndg, error {}. Probably the daemon will mess up our cgroups.'.format(success))
            except OSError as e:
                pass
                #print('libcgroup is not available: {}'.format(e.strerror))

            for cgroup in cgroups.values():
                addTaskToCgroup(cgroup, pid)


        # copy parent-environment and set needed values, either override or append
        runningEnv = os.environ.copy()
        for key, value in environments.get("newEnv", {}).items():
            runningEnv[key] = value
        for key, value in environments.get("additionalEnv", {}).items():
            runningEnv[key] = runningEnv.get(key, "") + value

        logging.debug("Using additional environment {0}.".format(str(environments)))

        # write command line into outputFile
        outputFile = open(outputFileName, 'w') # override existing file
        outputFile.write(' '.join(args) + '\n\n\n' + '-' * 80 + '\n\n\n')
        outputFile.flush()

        timelimitThread = None
        oomThread = None
        energyBefore = Util.getEnergy()
        wallTimeBefore = time.time()

        p = None
        try:
            p = subprocess.Popen(args,
                                 stdout=outputFile, stderr=outputFile,
                                 env=runningEnv, cwd=runningDir,
                                 preexec_fn=preSubprocess)

        except OSError as e:
            logging.critical("OSError {0} while starting {1}: {2}. "
                             + "Assure that the directory containing the tool to be benchmarked is included "
                             + "in the PATH environment variable or an alias is set."
                             .format(e.errno, args[0], e.strerror))
            return (0, 0, 0, None)

        try:
            with self.SUB_PROCESSES_LOCK:
                self.SUB_PROCESSES.add(p)

            if TIMELIMIT in rlimits and CPUACCT in cgroups:
                # Start a timer to periodically check timelimit with cgroup
                # if the tool uses subprocesses and ulimit does not work.
                timelimitThread = _TimelimitThread(cgroups[CPUACCT], rlimits[TIMELIMIT], p, myCpuCount)
                timelimitThread.start()

            if MEMLIMIT in rlimits:
                try:
                    oomThread = oomhandler.KillProcessOnOomThread(cgroups[MEMORY], p, rlimits[MEMLIMIT])
                    oomThread.start()
                except OSError as e:
                    logging.critical("OSError {0} during setup of OomEventListenerThread: {1}.".format(e.errno, e.strerror))

            try:
                logging.debug("waiting for: pid:{0}".format(p.pid))
                pid, returnvalue, ru_child = os.wait4(p.pid, 0)
                logging.debug("waiting finished: pid:{0}, retVal:{1}".format(pid, returnvalue))

            except OSError as e:
                returnvalue = 0
                ru_child = None
                logging.critical("OSError {0} while waiting for termination of {1} ({2}): {3}.".format(e.errno, args[0], p.pid, e.strerror))

        finally:
            with self.SUB_PROCESSES_LOCK:
                self.SUB_PROCESSES.discard(p)

            if timelimitThread:
                timelimitThread.cancel()

            if oomThread:
                oomThread.cancel()

            outputFile.close() # normally subprocess closes file, we do this again

            logging.debug("size of logfile '{0}': {1}".format(outputFileName, str(os.path.getsize(outputFileName))))

            # kill all remaining processes if some managed to survive
            for cgroup in cgroups.values():
                killAllTasksInCgroup(cgroup)

        wallTimeAfter = time.time()
        energy = Util.getEnergy(energyBefore)
        wallTime = wallTimeAfter - wallTimeBefore
        cpuTime = ru_child.ru_utime + ru_child.ru_stime if ru_child else 0
        return (returnvalue, wallTime, cpuTime, energy)



    def _getExactMeasures(self, cgroups, returnvalue, wallTime, cpuTime):
        """
        This method tries to extract better measures from cgroups.
        """
    
        cpuTime2 = None
        if CPUACCT in cgroups:
            # We want to read the value from the cgroup.
            # The documentation warns about outdated values.
            # So we read twice with 0.1s time difference,
            # and continue reading as long as the values differ.
            # This has never happened except when interrupting the script with Ctrl+C,
            # but just try to be on the safe side here.
            cgroupCpuacct = cgroups[CPUACCT]
            tmp = _readCpuTime(cgroupCpuacct)
            tmp2 = None
            while tmp != tmp2:
                time.sleep(0.1)
                tmp2 = tmp
                tmp = _readCpuTime(cgroupCpuacct)
            cpuTime2 = tmp

        memUsage = None
        if MEMORY in cgroups:
            # This measurement reads the maximum number of bytes of RAM+Swap the process used.
            # For more details, c.f. the kernel documentation:
            # https://www.kernel.org/doc/Documentation/cgroups/memory.txt
            memUsageFile = 'memory.memsw.max_usage_in_bytes'
            if not os.path.exists(os.path.join(cgroups[MEMORY], memUsageFile)):
                memUsageFile = 'memory.max_usage_in_bytes'
            if not os.path.exists(os.path.join(cgroups[MEMORY], memUsageFile)):
                logging.warning('Memory-usage is not available due to missing files.')
            else:
                try:
                    memUsage = readFile(cgroups[MEMORY], memUsageFile)
                    memUsage = int(memUsage)
                except IOError as e:
                    if e.errno == 95: # kernel responds with error 95 (operation unsupported) if this is disabled
                        logging.critical("Kernel does not track swap memory usage, cannot measure memory usage. "
                              + "Please set swapaccount=1 on your kernel command line.")
                    else:
                        raise e

        logging.debug('Run exited with code {0}, walltime={1}, cputime={2}, cgroup-cputime={3}, memory={4}'
                      .format(returnvalue, wallTime, cpuTime, cpuTime2, memUsage))

        # Usually cpuTime2 seems to be 0.01s greater than cpuTime.
        # Furthermore, cpuTime might miss some subprocesses,
        # therefore we expect cpuTime2 to be always greater (and more correct).
        # However, sometimes cpuTime is a little bit bigger than cpuTime2.
        # This may indicate a problem with cgroups, for example another process
        # moving our benchmarked process between cgroups.
        if cpuTime2 is not None:
            if (cpuTime * 0.9) > cpuTime2:
                logging.warning('Cputime measured by wait was {0}, cputime measured by cgroup was only {1}, perhaps measurement is flawed.'.format(cpuTime, cpuTime2))
            else:
                cpuTime = cpuTime2

        return (cpuTime, memUsage)


    def executeRun(self, args, rlimits, outputFileName, myCpuIndex=None, environments={}, runningDir=None, maxLogfileSize=-1):
        """
        This function executes a given command with resource limits,
        and writes the output to a file.
        @param args: the command line to run
        @param rlimits: the resource limits
        @param outputFileName: the file where the output should be written to
        @param myCpuIndex: None or the number of the first CPU core to use
        @param environments: special environments for running the command
        @return: a tuple with wallTime in seconds, cpuTime in seconds, memory usage in bytes, returnvalue, and process output
        """

        logging.debug("executeRun: setting up CCgoups.")
        (cgroups, myCpuCount) = self._setupCGroups(args, rlimits, myCpuIndex)

        logging.debug("executeRun: executing tool.")
        (returnvalue, wallTime, cpuTime, energy) = \
            self._execute(args, rlimits, outputFileName, cgroups, myCpuCount, environments, runningDir)

        logging.debug("executeRun: getting exact measures.")
        (cpuTime, memUsage) = self._getExactMeasures(cgroups, returnvalue, wallTime, cpuTime)

        logging.debug("executeRun: cleaning up CGroups.")
        for cgroup in set(cgroups.values()):
            # Need the set here to delete each cgroup only once.
            removeCgroup(cgroup)

        logging.debug("executeRun: reading output.")
        outputFile = open(outputFileName, 'rt') # re-open file for reading output
        output = list(map(Util.decodeToString, outputFile.readlines()))
        outputFile.close()

        logging.debug("executeRun: analysing output for crash-info.")
        getDebugOutputAfterCrash(output, outputFileName)

        output = reduceFileSize(outputFileName, output, maxLogfileSize)
        
        output = output[6:] # first 6 lines are for logging, rest is output of subprocess


        logging.debug("executeRun: Run execution returns with code {0}, walltime={1}, cputime={2}, memory={3}, energy={4}"
                      .format(returnvalue, wallTime, cpuTime, memUsage, energy))

        return (wallTime, cpuTime, memUsage, returnvalue, '\n'.join(output), energy)


    def kill(self):
        self.PROCESS_KILLED = True
        with self.SUB_PROCESSES_LOCK:
            for process in self.SUB_PROCESSES:
                logging.warn('Killing process {0} forcefully.'.format(process.pid))
                Util.killProcess(process.pid)

def reduceFileSize(outputFileName, output, maxLogfileSize=-1):
    """
    This function shrinks the logfile-content and returns the modified content.
    We remove only the middle part of a file,
    the file-start and the file-end remain unchanged.
    """
    if maxLogfileSize == -1: return output # disabled, nothing to do

    rest = maxLogfileSize * _BYTE_FACTOR * _BYTE_FACTOR # as MB, we assume: #char == #byte

    if sum(len(line) for line in output) < rest: return output # too small, nothing to do

    logging.warning("Logfile '{0}' too big. Removing lines.".format(outputFileName))

    half = len(output)/2
    newOutput = ([],[])
    # iterate parallel from start and end
    for lineFront, lineEnd in zip(output[:half], reversed(output[half:])):
        if len(lineFront) > rest: break
        newOutput[0].append(lineFront)
        if len(lineEnd) > rest: break
        newOutput[1].insert(0,lineEnd)
        rest = rest - len(lineFront) - len(lineEnd)

    # build new content and write to file
    output = newOutput[0] + ["\n\n\nWARNING: YOUR LOGFILE WAS TOO LONG, SOME LINES IN THE MIDDLE WERE REMOVED.\n\n\n"] + newOutput[1]
    writeFile(''.join(output).encode('utf-8'), outputFileName)

    return output


def getDebugOutputAfterCrash(output, outputFileName):
    """
    Segmentation faults and some memory failures reference a file 
    with more information. We append this file to the log.
    """
    next = False
    for line in output:
        if next:
            try:
                dumpFile = line.strip(' #\n')
                Util.appendFileToFile(dumpFile, outputFileName)
                os.remove(dumpFile)
            except IOError as e:
                logging.warn('Could not append additional segmentation fault information from {0} ({1})'.format(dumpFile, e.strerror))
            break
        if line.startswith('# An error report file with more information is saved as:'):
            logging.debug('Going to append error report file')
            next = True


def _readCpuTime(cgroupCpuacct):
    cputimeFile = os.path.join(cgroupCpuacct, 'cpuacct.usage')
    if not os.path.exists(cputimeFile):
        logging.warning('Could not read cputime. File {0} does not exist.'.format(cputimeFile))
        return 0 # dummy value, if cputime is not available
    return float(readFile(cputimeFile))/1000000000 # nano-seconds to seconds


class _TimelimitThread(threading.Thread):
    """
    Thread that periodically checks whether the given process has already
    reached its timelimit. After this happens, the process is terminated.
    """
    def __init__(self, cgroupCpuacct, timelimit, process, cpuCount=1):
        super(_TimelimitThread, self).__init__()
        daemon = True
        self.cgroupCpuacct = cgroupCpuacct
        self.timelimit = timelimit
        self.latestKillTime = time.time() + timelimit + _WALLTIME_LIMIT_OVERHEAD
        self.cpuCount = cpuCount
        self.process = process
        self.finished = threading.Event()

    def run(self):
        while not self.finished.is_set():
            read = False
            while not read:
                try:
                    usedCpuTime = _readCpuTime(self.cgroupCpuacct)
                    read = True
                except ValueError:
                    # Sometimes the kernel produces strange values with linebreaks in them
                    time.sleep(1)
                    pass
            remainingCpuTime = self.timelimit - usedCpuTime
            remainingWallTime = self.latestKillTime - time.time()
            logging.debug("TimelimitThread for process {0}: used cpu time: {1}, remaining cpu time: {2}, remaining wall time: {3}."
                          .format(self.process.pid, usedCpuTime, remainingCpuTime, remainingWallTime))
            if remainingCpuTime <= 0 or remainingWallTime <= 0:
                logging.debug('Killing process {0} due to timeout.'.format(self.process.pid))
                Util.killProcess(self.process.pid)
                self.finished.set()
                return

            remainingTime = max(remainingCpuTime/self.cpuCount, remainingWallTime)
            self.finished.wait(remainingTime + 1)

    def cancel(self):
        self.finished.set()


def _hasSwap():
    with open('/proc/meminfo', 'r') as meminfo:
        for line in meminfo:
            if line.startswith('SwapTotal:'):
                swap = line.split()[1]
                if int(swap) == 0:
                    return False
    return True
