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
from __future__ import absolute_import, division, print_function, unicode_literals

import logging
import multiprocessing
import os
import resource
import subprocess
import sys
import threading
import time

from . import util as Util
from .cgroups import *
from . import oomhandler

readFile = Util.readFile
writeFile = Util.writeFile

CPUACCT = 'cpuacct'
CPUSET = 'cpuset'
MEMORY = 'memory'

_WALLTIME_LIMIT_DEFAULT_OVERHEAD = 30 # seconds more than cputime limit


class RunExecutor():

    def __init__(self):
        self.PROCESS_KILLED = False
        self.SUB_PROCESSES_LOCK = threading.Lock() # needed, because we kill the process asynchronous
        self.SUB_PROCESSES = set()
        self._terminationReason = None

        self._initCGroups()


    def _initCGroups(self):
        """
        This function initializes the cgroups for the limitations.
        Please call it before any calls to executeRun(),
        if you want to separate initialization from actual run execution
        (e.g., for better error message handling).
        """
        self.cgroupsParents = {} # contains the roots of all cgroup-subsystems

        initCgroup(self.cgroupsParents, CPUACCT)
        if not self.cgroupsParents[CPUACCT]:
            logging.warning('Without cpuacct cgroups, cputime measurement and limit might not work correctly if subprocesses are started.')

        initCgroup(self.cgroupsParents, MEMORY)
        if not self.cgroupsParents[MEMORY]:
            logging.warning('Cannot measure memory consumption without memory cgroup.')

        initCgroup(self.cgroupsParents, CPUSET)

        self.cpus = None # to indicate that we cannot limit cores
        self.memoryNodes = None # to indicate that we cannot limit cores
        cgroupCpuset = self.cgroupsParents[CPUSET]
        if cgroupCpuset:
            # Read available cpus/memory nodes:
            cpuStr = readFile(cgroupCpuset, 'cpuset.cpus')
            try:
                self.cpus = Util.parseIntList(cpuStr)
            except ValueError as e:
                logging.warning("Could not read available CPU cores from kernel: {0}".format(e.strerror))
            logging.debug("List of available CPU cores is {0}.".format(self.cpus))

            memsStr = readFile(cgroupCpuset, 'cpuset.mems')
            try:
                self.memoryNodes = Util.parseIntList(memsStr)
            except ValueError as e:
                logging.warning("Could not read available memory nodes from kernel: {0}".format(e.strerror))
            logging.debug("List of available memory nodes is {0}.".format(self.memoryNodes))


    def _setupCGroups(self, args, myCpus, memlimit, memoryNodes):
        """
        This method creates the CGroups for the following execution.
        @param args: the command line to run, used only for logging
        @param myCpus: None or a list of the CPU cores to use
        @param memlimit: None or memory limit in bytes
        @param memoryNodes: None or a list of memory nodes of a NUMA system to use
        @return cgroups: a map of all the necessary cgroups for the following execution.
                         Please add the process of the following execution to all those cgroups!
        """
      
        # Setup cgroups, need a single call to createCgroup() for all subsystems
        subsystems = [CPUACCT, MEMORY]
        if myCpus is not None:
            subsystems.append(CPUSET)

        cgroups = createCgroup(self.cgroupsParents, *subsystems)

        logging.debug("Executing {0} in cgroups {1}.".format(args, cgroups.values()))

        # Setup cpuset cgroup if necessary to limit the CPU cores/memory nodes to be used.
        if myCpus is not None:
            cgroupCpuset = cgroups[CPUSET]
            myCpusStr = ','.join(map(str, myCpus))
            writeFile(myCpusStr, cgroupCpuset, 'cpuset.cpus')
            myCpusStr = readFile(cgroupCpuset, 'cpuset.cpus')
            logging.debug('Executing {0} with cpu cores [{1}].'.format(args, myCpusStr))

        if memoryNodes is not None:
            cgroupCpuset = cgroups[CPUSET]
            writeFile(','.join(map(str, memoryNodes)), cgroupCpuset, 'cpuset.mems')
            memoryNodesStr = readFile(cgroupCpuset, 'cpuset.mems')
            logging.debug('Executing {0} with memory nodes [{1}].'.format(args, memoryNodesStr))


        # Setup memory limit
        if memlimit is not None:
            cgroupMemory = cgroups[MEMORY]

            limitFile = 'memory.limit_in_bytes'
            writeFile(str(memlimit), cgroupMemory, limitFile)

            swapLimitFile = 'memory.memsw.limit_in_bytes'
            # We need swap limit because otherwise the kernel just starts swapping
            # out our process if the limit is reached.
            # Some kernels might not have this feature,
            # which is ok if there is actually no swap.
            if not os.path.exists(os.path.join(cgroupMemory, swapLimitFile)):
                if _hasSwap():
                    sys.exit('Kernel misses feature for accounting swap memory, but machine has swap. Please set swapaccount=1 on your kernel command line or disable swap with "sudo swapoff -a".')
            else:
                try:
                    writeFile(str(memlimit), cgroupMemory, swapLimitFile)
                except IOError as e:
                    if e.errno == 95: # kernel responds with error 95 (operation unsupported) if this is disabled
                        sys.exit('Memory limit specified, but kernel does not allow limiting swap memory. Please set swapaccount=1 on your kernel command line or disable swap with "sudo swapoff -a".')
                    raise e

            memlimit = readFile(cgroupMemory, limitFile)
            logging.debug('Executing {0} with memory limit {1} bytes.'.format(args, memlimit))

        if not os.path.exists(os.path.join(cgroups[MEMORY], 'memory.memsw.max_usage_in_bytes')) and _hasSwap():
            logging.warning('Kernel misses feature for accounting swap memory, but machine has swap. Memory usage may be measured inaccurately. Please set swapaccount=1 on your kernel command line or disable swap with "sudo swapoff -a".')

        return cgroups


    def _execute(self, args, outputFileName, cgroups, hardtimelimit, softtimelimit, walltimelimit, myCpuCount, memlimit, environments, workingDir):
        """
        This method executes the command line and waits for the termination of it. 
        """

        def preSubprocess():
            os.setpgrp() # make subprocess to group-leader
            os.nice(5) # increase niceness of subprocess

            if hardtimelimit is not None:
                # Also use ulimit for CPU time limit as a fallback if cgroups are not available
                resource.setrlimit(resource.RLIMIT_CPU, (hardtimelimit, hardtimelimit))
                # TODO: using ulimit allows the tool to be killed because of timelimit
                # without the termination reason to be properly set

            # put us into the cgroup(s)
            pid = os.getpid()
            # On some systems, cgrulesengd would move our process into other cgroups.
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

            for cgroup in set(cgroups.values()):
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
                                 env=runningEnv, cwd=workingDir,
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

            if hardtimelimit is not None and CPUACCT in cgroups:
                # Start a timer to periodically check timelimit with cgroup
                # if the tool uses subprocesses and ulimit does not work.
                timelimitThread = _TimelimitThread(cgroups[CPUACCT], hardtimelimit, softtimelimit, walltimelimit, p, myCpuCount, self._setTerminationReason)
                timelimitThread.start()

            if memlimit is not None:
                try:
                    oomThread = oomhandler.KillProcessOnOomThread(cgroups[MEMORY], p,
                                                                  self._setTerminationReason)
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
                if self.PROCESS_KILLED:
                    # OSError 4 (interrupted system call) seems always to happen if we killed the process ourselves after Ctrl+C was pressed
                    logging.debug("OSError {0} while waiting for termination of {1} ({2}): {3}.".format(e.errno, args[0], p.pid, e.strerror))
                else:
                    logging.critical("OSError {0} while waiting for termination of {1} ({2}): {3}.".format(e.errno, args[0], p.pid, e.strerror))

        finally:
            wallTimeAfter = time.time()
            

            with self.SUB_PROCESSES_LOCK:
                self.SUB_PROCESSES.discard(p)

            if timelimitThread:
                timelimitThread.cancel()

            if oomThread:
                oomThread.cancel()

            outputFile.close() # normally subprocess closes file, we do this again

            logging.debug("size of logfile '{0}': {1}".format(outputFileName, str(os.path.getsize(outputFileName))))

            # kill all remaining processes if some managed to survive
            for cgroup in set(cgroups.values()):
                killAllTasksInCgroup(cgroup)

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


    def executeRun(self, args, outputFileName,
                   hardtimelimit=None, softtimelimit=None, walltimelimit=None,
                   cores=None, memlimit=None, memoryNodes=None,
                   environments={}, workingDir=None, maxLogfileSize=None):
        """
        This function executes a given command with resource limits,
        and writes the output to a file.
        @param args: the command line to run
        @param outputFileName: the file where the output should be written to
        @param hardtimelimit: None or the CPU time in seconds after which the tool is forcefully killed.
        @param softtimelimit: None or the CPU time in seconds after which the tool is sent a kill signal.
        @param walltimelimit: None or the wall time in seconds after which the tool is forcefully killed (default: hardtimelimit + a few seconds)
        @param cores: None or a list of the CPU cores to use
        @param memlimit: None or memory limit in bytes
        @param memoryNodes: None or a list of memory nodes in a NUMA system to use
        @param environments: special environments for running the command
        @param workingDir: None or a directory which the execution should use as working directory
        @param maxLogfileSize: None or a number of bytes to which the output of the tool should be truncated approximately if there is too much output.
        @return: a tuple with wallTime in seconds, cpuTime in seconds, memory usage in bytes, returnvalue, and process output
        """

        if hardtimelimit is not None:
            if hardtimelimit <= 0:
                sys.exit("Invalid time limit {0}.".format(hardtimelimit))
        if softtimelimit is not None:
            if softtimelimit <= 0:
                sys.exit("Invalid soft time limit {0}.".format(softtimelimit))
            if hardtimelimit is None:
                sys.exit("Soft time limit without hard time limit is not implemented.")
            if softtimelimit > hardtimelimit:
                sys.exit("Soft time limit cannot be larger than the hard time limit.")

        if walltimelimit is None:
            if hardtimelimit is not None:
                walltimelimit = hardtimelimit + _WALLTIME_LIMIT_DEFAULT_OVERHEAD
        else:
            if walltimelimit <= 0:
                sys.exit("Invalid wall time limit {0}.".format(walltimelimit))
            if hardtimelimit is None:
                sys.exit("Wall time limit without hard time limit is not implemented.")

        if cores is not None:
            if self.cpus is None:
                sys.exit("Cannot limit CPU cores without cpuset cgroup.")
            coreCount = len(cores)
            if coreCount == 0:
                sys.exit("Cannot execute run without any CPU core.")
            if not set(cores).issubset(self.cpus):
                sys.exit("Cores {0} are not allowed to be used".format(list(set(cores).difference(self.cpus))))
        else:
            try:
                coreCount = multiprocessing.cpu_count()
            except NotImplementedError:
                coreCount = 1

        if memlimit is not None:
            if memlimit <= 0:
                sys.exit("Invalid memory limit {0}.".format(memlimit))
            if not self.cgroupsParents[MEMORY]:
                sys.exit("Memory limit specified, but cannot be implemented without cgroup support.")

        if memoryNodes is not None:
            if self.memoryNodes is None:
                sys.exit("Cannot restrict memory nodes without cpuset cgroup.")
            if len(memoryNodes) == 0:
                sys.exit("Cannot execute run without any memory node.")
            if not set(memoryNodes).issubset(self.memoryNodes):
                sys.exit("Memory nodes {0} are not allowed to be used".format(list(set(memoryNodes).difference(self.memoryNodes))))

        if workingDir:
            if not os.path.exists(workingDir):
                sys.exit("Working directory {0} does not exist.".format(workingDir))
            if not os.path.isdir(workingDir):
                sys.exit("Working directory {0} is not a directory.".format(workingDir))
            if not os.access(workingDir, os.X_OK):
                sys.exit("Permission denied for working directory {0}.".format(workingDir))

        self._terminationReason = None

        logging.debug("executeRun: setting up Cgroups.")
        cgroups = self._setupCGroups(args, cores, memlimit, memoryNodes)

        try:
            logging.debug("executeRun: executing tool.")
            (exitcode, wallTime, cpuTime, energy) = \
                self._execute(args, outputFileName, cgroups,
                              hardtimelimit, softtimelimit, walltimelimit,
                              coreCount, memlimit,
                              environments, workingDir)

            logging.debug("executeRun: getting exact measures.")
            (cpuTime, memUsage) = self._getExactMeasures(cgroups, exitcode, wallTime, cpuTime)

        finally: # always try to cleanup cgroups, even on sys.exit()
            logging.debug("executeRun: cleaning up CGroups.")
            for cgroup in set(cgroups.values()):
                # Need the set here to delete each cgroup only once.
                removeCgroup(cgroup)

        # if exception is thrown, skip the rest, otherwise perform normally

        _reduceFileSizeIfNecessary(outputFileName, maxLogfileSize)

        if exitcode not in [0,1]:
            logging.debug("executeRun: analysing output for crash-info.")
            _getDebugOutputAfterCrash(outputFileName)

        logging.debug("executeRun: Run execution returns with code {0}, walltime={1}, cputime={2}, memory={3}, energy={4}"
                      .format(exitcode, wallTime, cpuTime, memUsage, energy))

        result = {'walltime': wallTime,
                  'cputime':  cpuTime,
                  'exitcode': exitcode,
                  }
        if memUsage:
            result['memory'] = memUsage
        if self._terminationReason:
            result['terminationReason'] = self._terminationReason
        if energy:
            result['energy'] = energy
        return result

    def _setTerminationReason(self, reason):
        self._terminationReason = reason

    def kill(self):
        self._setTerminationReason('killed')
        self.PROCESS_KILLED = True
        with self.SUB_PROCESSES_LOCK:
            for process in self.SUB_PROCESSES:
                logging.warn('Killing process {0} forcefully.'.format(process.pid))
                Util.killProcess(process.pid)


def _reduceFileSizeIfNecessary(fileName, maxSize):
    """
    This function shrinks a file.
    We remove only the middle part of a file,
    the file-start and the file-end remain unchanged.
    """
    if maxSize is None: return # disabled, nothing to do

    fileSize = os.path.getsize(fileName)
    if fileSize < (maxSize + 500): return # not necessary

    logging.warning("Logfile '{0}' is too big (size {1} bytes). Removing lines.".format(fileName, fileSize))

    # We partition the file into 3 parts:
    # A) start: maxSize/2 bytes we want to keep
    # B) middle: part we want to remove
    # C) end: maxSize/2 bytes we want to keep

    # Trick taken from StackOverflow:
    # https://stackoverflow.com/questions/2329417/fastest-way-to-delete-a-line-from-large-file-in-python
    # We open the file twice at the same time, once for reading and once for writing.
    # We position the one file object at the beginning of B
    # and the other at the beginning of C.
    # Then we copy the content of C into B, overwriting what is there.
    # Afterwards we truncate the file after A+C.

    with open(fileName, 'r+') as outputFile:
        with open(fileName, 'r') as inputFile:
            # Position outputFile between A and B
            outputFile.seek(maxSize // 2)
            outputFile.readline() # jump to end of current line so that we truncate at line boundaries

            outputFile.write("\n\n\nWARNING: YOUR LOGFILE WAS TOO LONG, SOME LINES IN THE MIDDLE WERE REMOVED.\n\n\n\n")

            # Position inputFile between B and C
            inputFile.seek(-maxSize // 2, os.SEEK_END) # jump to beginning of second part we want to keep from end of file
            inputFile.readline() # jump to end of current line so that we truncate at line boundaries

            # Copy C over B
            _copyAllLinesFromTo(inputFile, outputFile)

            outputFile.truncate()


def _copyAllLinesFromTo(inputFile, outputFile):
    """
    Copy all lines from an input file object to an output file object.
    """
    currentLine = inputFile.readline()
    while currentLine:
        outputFile.write(currentLine)
        currentLine = inputFile.readline()


def _getDebugOutputAfterCrash(outputFileName):
    """
    Segmentation faults and some memory failures reference a file 
    with more information (hs_err_pid_*). We append this file to the log.
    The format that we expect is a line
    "# An error report file with more information is saved as:"
    and the file name of the dump file on the next line.
    """
    foundDumpFile = False
    with open(outputFileName, 'r+') as outputFile:
        for line in outputFile:
            if foundDumpFile:
                try:
                    dumpFileName = line.strip(' #\n')
                    outputFile.seek(0, os.SEEK_END) # jump to end of log file
                    with open(dumpFileName, 'r') as dumpFile:
                        _copyAllLinesFromTo(dumpFile, outputFile)
                    os.remove(dumpFileName)
                except IOError as e:
                    logging.warn('Could not append additional segmentation fault information from {0} ({1})'.format(dumpFile, e.strerror))
                break
            if unicode(line, errors='ignore').startswith('# An error report file with more information is saved as:'):
                logging.debug('Going to append error report file')
                foundDumpFile = True


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
    def __init__(self, cgroupCpuacct, hardtimelimit, softtimelimit, walltimelimit, process, cpuCount=1,
                 callbackFn=lambda reason: None):
        super(_TimelimitThread, self).__init__()
        self.daemon = True
        self.cgroupCpuacct = cgroupCpuacct
        self.timelimit = hardtimelimit
        self.softtimelimit = softtimelimit or hardtimelimit
        self.latestKillTime = time.time() + walltimelimit
        self.cpuCount = cpuCount
        self.process = process
        self.callback = callbackFn
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
            logging.debug("TimelimitThread for process {0}: used CPU time: {1}, remaining CPU time: {2}, remaining wall time: {3}."
                          .format(self.process.pid, usedCpuTime, remainingCpuTime, remainingWallTime))
            if remainingCpuTime <= 0:
                self.callback('cputime')
                logging.debug('Killing process {0} due to CPU time timeout.'.format(self.process.pid))
                Util.killProcess(self.process.pid)
                self.finished.set()
                return
            if remainingWallTime <= 0:
                self.callback('walltime')
                logging.warning('Killing process {0} due to wall time timeout.'.format(self.process.pid))
                Util.killProcess(self.process.pid)
                self.finished.set()
                return

            if (self.softtimelimit - usedCpuTime) <= 0:
                self.callback('cputime-soft')
                # soft time limit violated, ask process to terminate
                Util.killProcess(self.process.pid, signal.SIGTERM)
                self.softtimelimit = self.timelimit

            remainingTime = min(remainingCpuTime/self.cpuCount, remainingWallTime)
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
