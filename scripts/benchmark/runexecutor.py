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

import benchmark.util as Util

MEMLIMIT = "memlimit"
TIMELIMIT = "timelimit"
CORELIMIT = "cpuCores"
CPUACCT = 'cpuacct'
CPUSET = 'cpuset'
MEMORY = 'memory'

_BYTE_FACTOR = 1024 # byte in kilobyte

_SUB_PROCESSES = set()
_SUB_PROCESSES_LOCK = threading.Lock()

# The list of available CPU cores
_cpus = []

def init():
    """
    This function initializes the module.
    Please call it before any calls to executeRun(),
    if you want to separate initialization from actual run execution
    (e.g., for better error message handling).
    """
    _initCgroup(CPUACCT)
    if not _cgroups[CPUACCT]:
        logging.warning('Without cpuacct cgroups, cputime measurement and limit might not work correctly if subprocesses are started.')

    _initCgroup(MEMORY)
    if not _cgroups[MEMORY]:
        logging.warning('Cannot measure and limit memory consumption without memory cgroups.')

    _initCgroup(CPUSET)

    cgroupCpuset = _cgroups[CPUSET]
    if not cgroupCpuset:
        logging.warning("Cannot limit the number of CPU curse without cpuset cgroup.")
    else:
        # Read available cpus:
        global _cpus
        cpuStr = _readFile(cgroupCpuset, 'cpuset.cpus')
        for cpu in cpuStr.split(','):
            cpu = cpu.split('-')
            if len(cpu) == 1:
                _cpus.append(int(cpu[0]))
            elif len(cpu) == 2:
                start, end = cpu
                _cpus.extend(range(int(start), int(end)+1))
            else:
                logging.warning("Could not read available CPU cores from kernel, failed to parse {0}.".format(cpuStr))

        logging.debug("List of available CPU cores is {0}.".format(_cpus))


def executeRun(args, rlimits, outputFileName, myCpuIndex=None):
    """
    This function executes a given command with resource limits,
    and writes the output to a file.
    @param args: the command line to run
    @param rlimits: the resource limits
    @param outputFileName: the file where the output should be written to
    @param myCpuIndex: None or the number of the first CPU core to use
    @param myCpuCount: None or the number of CPU cores to use
    @return: a tuple with wallTime in seconds, cpuTime in seconds, memory usage in bytes, returnvalue, and process output
    """
    def preSubprocess():
        os.setpgrp() # make subprocess to group-leader

        if TIMELIMIT in rlimits:
            # Also use ulimit for CPU time limit as a fallback if cgroups are not available
            resource.setrlimit(resource.RLIMIT_CPU, (rlimits[TIMELIMIT], rlimits[TIMELIMIT]))

        # put us into the cgroup(s)
        pid = os.getpid()
        for cgroup in cgroups.values():
            _addTaskToCgroup(cgroup, pid)

    # Setup cgroups, need a single call to _createCgroup() for all subsystems
    subsystems = [CPUACCT, MEMORY]
    if CORELIMIT in rlimits and myCpuIndex is not None:
        subsystems.append(CPUSET)
    cgroups = _createCgroup(*subsystems)

    logging.debug("Executing {0} in cgroups {1}.".format(args, cgroups.values()))

    try:
        myCpuCount = multiprocessing.cpu_count()
    except NotImplementedError:
        myCpuCount = 1

    # Setup cpuset cgroup if necessary to limit the CPU cores to be used.
    if CORELIMIT in rlimits and myCpuIndex is not None:
        myCpuCount = rlimits[CORELIMIT]
        if not _cpus or CPUSET not in cgroups:
            sys.exit("Cannot limit number of CPU cores because cgroups are not available.")
        if myCpuCount > len(_cpus):
            sys.exit("Cannot execute runs on {0} CPU cores, only {1} are available.".format(myCpuCount, len(_cpus)))
        if myCpuCount <= 0:
            sys.exit("Invalid number of CPU cores to use: {0}".format(myCpuCount))

        cgroupCpuset = cgroups[CPUSET]
        totalCpuCount = len(_cpus)
        myCpusStart = (myCpuIndex * myCpuCount) % totalCpuCount
        myCpusEnd   = (myCpusStart + myCpuCount-1) % totalCpuCount
        myCpus = ','.join(map(str, range(myCpusStart, myCpusEnd+1)))
        _writeFile(myCpus, cgroupCpuset, 'cpuset.cpus')

        myCpus = _readFile(cgroupCpuset, 'cpuset.cpus')
        logging.debug('Executing {0} with cpu cores {1}.'.format(args, myCpus))

    # Setup memory limit
    if MEMLIMIT in rlimits:
        if not MEMORY in cgroups:
            sys.exit("Memory limit specified, but cannot be implemented without cgroup support.")
        cgroupMemory = cgroups[MEMORY]
        memlimit = str(rlimits[MEMLIMIT] * _BYTE_FACTOR * _BYTE_FACTOR) # MB to Byte
        _writeFile(memlimit, cgroupMemory, 'memory.limit_in_bytes')
        try:
            _writeFile(memlimit, cgroupMemory, 'memory.memsw.limit_in_bytes')
        except IOError as e:
            if e.errno == 95: # kernel responds with error 95 (operation unsupported) if this is disabled
                sys.exit("Memory limit specified, but kernel does not allow limiting swap memory. Please set swapaccount=1 on your kernel command line.")
            raise e

        memlimit = _readFile(cgroupMemory, 'memory.memsw.limit_in_bytes')
        logging.debug('Executing {0} with memory limit {1} bytes.'.format(args, memlimit))

    outputFile = open(outputFileName, 'w') # override existing file
    outputFile.write(' '.join(args) + '\n\n\n' + '-'*80 + '\n\n\n')
    outputFile.flush()

    registeredSubprocess = False
    timelimitThread = None
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

        if TIMELIMIT in rlimits and CPUACCT in cgroups:
            # Start a timer to periodically check timelimit with cgroup
            # if the tool uses subprocesses and ulimit does not work.
            timelimitThread = _TimelimitThread(cgroups[CPUACCT], rlimits[TIMELIMIT], p, myCpuCount)
            timelimitThread.start()

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

        if timelimitThread:
            timelimitThread.cancel()

        outputFile.close() # normally subprocess closes file, we do this again

        # kill all remaining processes if some managed to survive
        for cgroup in cgroups.values():
            _killAllTasksInCgroup(cgroup)

    assert pid == p.pid

    wallTimeAfter = time.time()
    wallTime = wallTimeAfter - wallTimeBefore
    cpuTime = (ru_child.ru_utime + ru_child.ru_stime)
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
        try:
            memUsage = _readFile(cgroups[MEMORY], 'memory.memsw.max_usage_in_bytes')
            memUsage = int(memUsage)
        except IOError as e:
            if e.errno == 95: # kernel responds with error 95 (operation unsupported) if this is disabled
                print("Kernel does not track swap memory usage, cannot measure memory usage. Please set swapaccount=1 on your kernel command line.")
            else:
                raise e

    for cgroup in set(cgroups.values()):
        # Need the set here to delete each cgroup only once.
        _removeCgroup(cgroup)

    logging.debug('Run exited with code {0}, walltime={1}, cputime={2}, cgroup-cputime={3}, memory={4}'.format(returnvalue, wallTime, cpuTime, cpuTime2, memUsage))

    # Usually cpuTime2 seems to be 0.01s greater than cpuTime.
    # Furthermore, cpuTime might miss some subprocesses,
    # therefore we expect cpuTime2 to be always greater (and more correct).
    # However, sometimes cpuTime is a little bit bigger than cpuTime2.
    if cpuTime2 is not None:
        if (cpuTime*0.999) > cpuTime2:
            logging.warning('Cputime measured by wait was {0}, cputime measured by cgroup was only {1}, perhaps measurement is flawed.'.format(cpuTime, cpuTime2))
        else:
            cpuTime = cpuTime2

    outputFile = open(outputFileName, 'rt') # re-open file for reading output
    output = list(map(Util.decodeToString, outputFile.readlines()[6:])) # first 6 lines are for logging, rest is output of subprocess
    outputFile.close()

    # Segmentation faults and some memory failures reference a file with more information.
    # We append this file to the log.
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

    return (wallTime, cpuTime, memUsage, returnvalue, '\n'.join(output))

def killAllProcesses():
    try:
        _SUB_PROCESSES_LOCK.acquire()
        for process in _SUB_PROCESSES:
            _killSubprocess(process)
    finally:
        _SUB_PROCESSES_LOCK.release()

def _readFile(*path):
    with open(os.path.join(*path)) as f:
        return f.read().strip()

def _writeFile(value, *path):
    with open(os.path.join(*path), 'w') as f:
        return f.write(value)

def _readCpuTime(cgroupCpuacct):
    return float(_readFile(cgroupCpuacct, 'cpuacct.usage'))/1000000000 # nano-seconds to seconds


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
        self.cpuCount = cpuCount
        self.process = process
        self.finished = threading.Event()

    def run(self): 
        while not self.finished.is_set():
            usedTime = _readCpuTime(self.cgroupCpuacct)
            if usedTime >= self.timelimit:
                _killSubprocess(self.process)
                self.finished.set()
                return

            remainingTime = self.timelimit - usedTime + 1 
            self.finished.wait(remainingTime/self.cpuCount)

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

_cgroups = {}

def _createCgroup(*subsystems):
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
        _initCgroup(subsystem)

        parentCgroup = _cgroups.get(subsystem)
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
    if cgroup:
        tasksFile = os.path.join(cgroup, 'tasks')
        while os.path.getsize(tasksFile) > 0:
            with open(tasksFile, 'rt') as tasks:
                for task in tasks:
                    logging.warning('Run has left-over process with pid {0}'.format(task))
                    try:
                        os.kill(int(task), signal.SIGKILL)
                    except OSError:
                        # task already terminated between reading and killing
                        pass

def _removeCgroup(cgroup):
    if cgroup:
        assert os.path.getsize(os.path.join(cgroup, 'tasks')) == 0
        try:
            os.rmdir(cgroup)
        except OSError:
            # somethings this fails because the cgroup is still busy, we try again once
            os.rmdir(cgroup)

def _initCgroup(subsystem):
    if not subsystem in _cgroups:
        cgroup = _findCgroupMount(subsystem)

        if not cgroup:
            logging.warning(
'''Cgroup subsystem {0} not enabled.
Please enable it with "sudo mount -t cgroup none /sys/fs/cgroup".'''
                .format(subsystem)
                )
            _cgroups[subsystem] = None
            return
        else:
            logging.debug('Subsystem {0} is mounted at {1}'.format(subsystem, cgroup))

        # find our own cgroup, we want to put processes in a child group of it
        cgroup = os.path.join(cgroup, _findOwnCgroup(subsystem)[1:])
        _cgroups[subsystem] = cgroup
        logging.debug('My cgroup for subsystem {0} is {1}'.format(subsystem, cgroup))

        try:
            testCgroup = _createCgroup(subsystem)[subsystem]
            _removeCgroup(testCgroup)

            logging.debug('Found {0} subsystem for cgroups mounted at {1}'.format(subsystem, cgroup))
        except OSError as e:
            logging.warning(
'''Cannot use cgroup hierarchy mounted at {0}, reason: {1}
If permissions are wrong, please run "sudo chmod o+wt \'{0}\'".'''
                .format(cgroup, e.strerror))
            _cgroups[subsystem] = None
