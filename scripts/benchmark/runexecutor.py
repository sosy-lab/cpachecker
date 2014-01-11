"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2013  Dirk Beyer
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
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import time

from . import util as Util
from . import filewriter

readFile = filewriter.readFile
writeFile = filewriter.writeFile

MEMLIMIT = "memlimit"
TIMELIMIT = "timelimit"
CORELIMIT = "cpuCores"
CPUACCT = 'cpuacct'
CPUSET = 'cpuset'
MEMORY = 'memory'

_BYTE_FACTOR = 1024 # byte in kilobyte
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

        _initCgroup(self.cgroupsParents, CPUACCT)
        if not self.cgroupsParents[CPUACCT]:
            logging.warning('Without cpuacct cgroups, cputime measurement and limit might not work correctly if subprocesses are started.')

        _initCgroup(self.cgroupsParents, MEMORY)
        if not self.cgroupsParents[MEMORY]:
            logging.warning('Cannot measure and limit memory consumption without memory cgroups.')

        _initCgroup(self.cgroupsParents, CPUSET)

        cgroupCpuset = self.cgroupsParents[CPUSET]
        if not cgroupCpuset:
            logging.warning("Cannot limit the number of CPU curse without cpuset cgroup.")
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
      
        # Setup cgroups, need a single call to _createCgroup() for all subsystems
        subsystems = [CPUACCT, MEMORY]
        if CORELIMIT in rlimits and myCpuIndex is not None:
            subsystems.append(CPUSET)
        cgroups = _createCgroup(self.cgroupsParents, *subsystems)

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
                _addTaskToCgroup(cgroup, pid)


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
            return (0, 0, 0)

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
                    oomThread = _OomEventThread(cgroups[MEMORY], p, rlimits[MEMLIMIT])
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
                _killAllTasksInCgroup(cgroup)

        wallTimeAfter = time.time()
        wallTime = wallTimeAfter - wallTimeBefore
        cpuTime = ru_child.ru_utime + ru_child.ru_stime if ru_child else 0
        return (returnvalue, wallTime, cpuTime)



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
        (returnvalue, wallTime, cpuTime) = \
            self._execute(args, rlimits, outputFileName, cgroups, myCpuCount, environments, runningDir)

        logging.debug("executeRun: getting exact measures.")
        (cpuTime, memUsage) = self._getExactMeasures(cgroups, returnvalue, wallTime, cpuTime)

        logging.debug("executeRun: cleaning up CGroups.")
        for cgroup in set(cgroups.values()):
            # Need the set here to delete each cgroup only once.
            _removeCgroup(cgroup)

        logging.debug("executeRun: reading output.")
        outputFile = open(outputFileName, 'rt') # re-open file for reading output
        output = list(map(Util.decodeToString, outputFile.readlines()))
        outputFile.close()

        logging.debug("executeRun: analysing output for crash-info.")
        getDebugOutputAfterCrash(output, outputFileName)

        output = reduceFileSize(outputFileName, output, maxLogfileSize)
        
        output = output[6:] # first 6 lines are for logging, rest is output of subprocess


        logging.debug("executeRun: Run execution returns with code {0}, walltime={1}, cputime={2}, memory={3}"
                      .format(returnvalue, wallTime, cpuTime, memUsage))

        return (wallTime, cpuTime, memUsage, returnvalue, '\n'.join(output))


    def kill(self):
        self.PROCESS_KILLED = True
        with self.SUB_PROCESSES_LOCK:
            for process in self.SUB_PROCESSES:
                _killSubprocess(process)

def reduceFileSize(outputFileName, output, maxLogfileSize=-1):
    """
    This function shrinks the logfile-content and returns the modified content.
    We remove only the middle part of a file,
    the file-start and the file-end remain unchanged.
    """
    if maxLogfileSize == -1: return output # disabled, nothing to do

    rest = maxLogfileSize * 1000 * 1000 # as MB, we assume: #char == #byte

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
    return float(readFile(cgroupCpuacct, 'cpuacct.usage'))/1000000000 # nano-seconds to seconds


class _OomEventThread(threading.Thread):
    """
    Thread that kills the process when they run out of memory.
    Usually the kernel would do this by itself,
    but sometimes the process still hangs because it does not even have
    enough memory left to get killed
    (the memory limit also effects some kernel-internal memory related to our process).
    So we disable the kernel-side killing,
    and instead let the kernel notify us via an event when the cgroup ran out of memory.
    Then we kill the process ourselves and increase the memory limit a little bit.
    
    The notification works by opening an "event file descriptor" with eventfd,
    and telling the kernel to notify us about OOMs by writing the event file
    descriptor and an file descriptor of the memory.oom_control file
    to cgroup.event_control.
    The kernel-side process killing is disabled by writing 1 to memory.oom_control.
    Sources:
    https://www.kernel.org/doc/Documentation/cgroups/memory.txt
    https://access.redhat.com/site/documentation//en-US/Red_Hat_Enterprise_Linux/6/html/Resource_Management_Guide/sec-memory.html#ex-OOM-control-notifications
    """
    def __init__(self, cgroup, process, memlimit):
        super(_OomEventThread, self).__init__()
        daemon = True
        self._finished = threading.Event()
        self._process = process
        self._memlimit = memlimit
        self._cgroup = cgroup

        ofd = os.open(os.path.join(cgroup, 'memory.oom_control'), os.O_WRONLY)
        try:
            from ctypes import cdll
            libc = cdll.LoadLibrary('libc.so.6')

            # Important to use CLOEXEC, otherwise the benchmarked tool inherits
            # the file descriptor.
            EFD_CLOEXEC = 0x80000 # from <sys/eventfd.h>
            self._efd = libc.eventfd(0, EFD_CLOEXEC) 

            try:
                writeFile('{} {}'.format(self._efd, ofd),
                          cgroup, 'cgroup.event_control')

                # If everything worked, disable Kernel-side process killing.
                # This is not allowed if memory.use_hierarchy is enabled,
                # but we don't care.
                try:
                    os.write(ofd, '1')
                except OSError:
                    pass
            except Error as e:
                os.close(self._efd)
                raise e
        finally:
            os.close(ofd)

    def run(self):
        try:
            # In an eventfd, there are always 8 bytes
            eventNumber = os.read(self._efd, 8) # blocks
            # If read returned, this means the kernel sent us an event.
            # It does so either on OOM or if the cgroup os removed.
            if not self._finished.is_set():
                logging.info('Killing process {0} due to out-of-memory event from kernel.'.format(self._process.pid))
                _killSubprocess(self._process)
                # Also kill all children of subprocesses directly.
                with open(os.path.join(self._cgroup, 'tasks'), 'rt') as tasks:
                    for task in tasks:
                        try:
                            os.kill(int(task), signal.SIGKILL)
                        except OSError:
                            # task already terminated between reading and killing
                            pass

                # We now need to increase the memory limit of this cgroup
                # to give the process a chance to terminate
                # 10MB ought to be enough
                limitFile = 'memory.memsw.limit_in_bytes'
                if not os.path.exists(os.path.join(self._cgroup, limitFile)):
                    limitFile = 'memory.limit_in_bytes'
                try:
                    writeFile(str((self._memlimit + 10) * _BYTE_FACTOR * _BYTE_FACTOR),
                              self._cgroup, limitFile)
                except IOError:
                    logging.warning('Failed to increase memory limit after OOM: error {0} ({1})'.format(e.errno, e.strerror))

        finally:
            os.close(self._efd)

    def cancel(self):
        self._finished.set()


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
                logging.info('Killing process {0} due to timeout.'.format(self.process.pid))
                _killSubprocess(self.process)
                self.finished.set()
                return

            remainingTime = max(remainingCpuTime/self.cpuCount, remainingWallTime)
            self.finished.wait(remainingTime + 1)

    def cancel(self):
        self.finished.set()


def _killSubprocess(process):
    '''
    this function kills the process and the children in its group.
    '''
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except OSError: # process itself returned and exited before killing
        pass

def _hasSwap():
    with open('/proc/meminfo', 'r') as meminfo:
        for line in meminfo:
            if line.startswith('SwapTotal:'):
                swap = line.split()[1]
                if int(swap) == 0:
                    return False
    return True

def _findCgroupMount(subsystem=None):
    with open('/proc/mounts', 'rt') as mounts:
        for mount in mounts:
            mount = mount.split(' ')
            if mount[2] == 'cgroup':
                mountpoint = mount[1]
                options = mount[3]
                logging.debug('Found cgroup mount at {0} with options {1}'.format(mountpoint, options))
                if subsystem:
                    if subsystem in options.split(','):
                        return mountpoint
                else:
                    return mountpoint
    return None


def _createCgroup(cgroupsParents, *subsystems):
    """
    Try to create a cgroup for each of the given subsystems.
    If multiple subsystems are available in the same hierarchy,
    a common cgroup for theses subsystems is used.
    @param subsystems: a list of cgroup subsystems
    @return a map from subsystem to cgroup for each subsystem where it was possible to create a cgroup
    """
    createdCgroupsPerSubsystem = {}
    createdCgroupsPerParent = {}
    for subsystem in subsystems:
        _initCgroup(cgroupsParents, subsystem)

        parentCgroup = cgroupsParents.get(subsystem)
        if not parentCgroup:
            # subsystem not enabled
            continue
        if parentCgroup in createdCgroupsPerParent:
            # reuse already created cgroup
            createdCgroupsPerSubsystem[subsystem] = createdCgroupsPerParent[parentCgroup]
            continue

        cgroup = tempfile.mkdtemp(prefix='benchmark_', dir=parentCgroup)
        createdCgroupsPerSubsystem[subsystem] = cgroup
        createdCgroupsPerParent[parentCgroup] = cgroup

        # add allowed cpus and memory to cgroup if necessary
        # (otherwise we can't add any tasks)
        try:
            shutil.copyfile(os.path.join(parentCgroup, 'cpuset.cpus'), os.path.join(cgroup, 'cpuset.cpus'))
            shutil.copyfile(os.path.join(parentCgroup, 'cpuset.mems'), os.path.join(cgroup, 'cpuset.mems'))
        except IOError:
            # expected to fail if cpuset subsystem is not enabled in this hierarchy
            pass

    return createdCgroupsPerSubsystem

def _findOwnCgroup(subsystem):
    """
    Given a cgroup subsystem,
    find the cgroup in which this process is in.
    (Each process is in exactly cgroup in each hierarchy.)
    @return the path to the cgroup inside the hierarchy
    """
    with open('/proc/self/cgroup', 'rt') as ownCgroups:
        for ownCgroup in ownCgroups:
            #each line is "id:subsystem,subsystem:path"
            ownCgroup = ownCgroup.strip().split(':')
            if subsystem in ownCgroup[1].split(','):
                return ownCgroup[2]
        logging.warning('Could not identify my cgroup for subsystem {0} although it should be there'.format(subsystem))
        return None

def _addTaskToCgroup(cgroup, pid):
    if cgroup:
        with open(os.path.join(cgroup, 'tasks'), 'w') as tasksFile:
            tasksFile.write(str(pid))

def _killAllTasksInCgroup(cgroup):
    tasksFile = os.path.join(cgroup, 'tasks')
    i = 1
    while i <= 2: # Do two triess of killing processes
        with open(tasksFile, 'rt') as tasks:
            task = None
            for task in tasks:
                logging.warning('Run has left-over process with pid {0}, killing it (try {1}).'.format(task, i))
                try:
                    os.kill(int(task), signal.SIGKILL)
                except OSError:
                    # task already terminated between reading and killing
                    pass

            if task is None:
                return # No process was hanging, exit
            elif i == 2:
                logging.warning('Run still has left over processes after second try of killing them, giving up.')
            i += 1

def _removeCgroup(cgroup):
    if cgroup:
        assert os.path.getsize(os.path.join(cgroup, 'tasks')) == 0
        try:
            os.rmdir(cgroup)
        except OSError:
            # sometimes this fails because the cgroup is still busy, we try again once
            os.rmdir(cgroup)

def _initCgroup(cgroupsParents, subsystem):
    if not subsystem in cgroupsParents:
        cgroup = _findCgroupMount(subsystem)

        if not cgroup:
            logging.warning(
'''Cgroup subsystem {0} not enabled.
Please enable it with "sudo mount -t cgroup none /sys/fs/cgroup".'''
                .format(subsystem)
                )
            cgroupsParents[subsystem] = None
            return
        else:
            logging.debug('Subsystem {0} is mounted at {1}'.format(subsystem, cgroup))

        # find our own cgroup, we want to put processes in a child group of it
        cgroup = os.path.join(cgroup, _findOwnCgroup(subsystem)[1:])
        cgroupsParents[subsystem] = cgroup
        logging.debug('My cgroup for subsystem {0} is {1}'.format(subsystem, cgroup))

        try: # only for testing?
            testCgroup = _createCgroup(cgroupsParents, subsystem)[subsystem]
            _removeCgroup(testCgroup)

            logging.debug('Found {0} subsystem for cgroups mounted at {1}'.format(subsystem, cgroup))
        except OSError as e:
            logging.warning(
'''Cannot use cgroup hierarchy mounted at {0}, reason: {1}
If permissions are wrong, please run "sudo chmod o+wt \'{0}\'".'''
                .format(cgroup, e.strerror))
            cgroupsParents[subsystem] = None
