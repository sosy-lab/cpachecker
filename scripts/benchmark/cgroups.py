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
import os
import shutil
import signal
import tempfile

from . import util as Util

CGROUP_NAME_PREFIX='benchmark_'

def initCgroup(cgroupsParents, subsystem):
    """
    Initialize a cgroup subsystem.
    Call this before calling any other methods from this module for this subsystem.
    @param cgroupsParents: A dictionary with the cgroup mount points for each subsystem (filled by this method)
    @param subsystem: The subsystem to initialize
    """
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
            testCgroup = createCgroup(cgroupsParents, subsystem)[subsystem]
            removeCgroup(testCgroup)

            logging.debug('Found {0} subsystem for cgroups mounted at {1}'.format(subsystem, cgroup))
        except OSError as e:
            logging.warning(
'''Cannot use cgroup hierarchy mounted at {0}, reason: {1}
If permissions are wrong, please run "sudo chmod o+wt \'{0}\'".'''
                .format(cgroup, e.strerror))
            cgroupsParents[subsystem] = None


def _findCgroupMount(subsystem=None):
    try:
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
    except:
        pass # /proc/mounts cannot be read
    return None


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


def createCgroup(cgroupsParents, *subsystems):
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
        initCgroup(cgroupsParents, subsystem)

        parentCgroup = cgroupsParents.get(subsystem)
        if not parentCgroup:
            # subsystem not enabled
            continue
        if parentCgroup in createdCgroupsPerParent:
            # reuse already created cgroup
            createdCgroupsPerSubsystem[subsystem] = createdCgroupsPerParent[parentCgroup]
            continue

        cgroup = tempfile.mkdtemp(prefix=CGROUP_NAME_PREFIX, dir=parentCgroup)
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

def addTaskToCgroup(cgroup, pid):
    if cgroup:
        with open(os.path.join(cgroup, 'tasks'), 'w') as tasksFile:
            tasksFile.write(str(pid))

def killAllTasksInCgroup(cgroup):
    tasksFile = os.path.join(cgroup, 'tasks')
    i = 1
    while i <= 2: # Do two triess of killing processes
        with open(tasksFile, 'rt') as tasks:
            task = None
            for task in tasks:
                logging.warning('Run has left-over process with pid {0}, killing it (try {1}).'.format(task, i))
                Util.killProcess(int(task), signal.SIGKILL)

            if task is None:
                return # No process was hanging, exit
            elif i == 2:
                logging.warning('Run still has left over processes after second try of killing them, giving up.')
            i += 1

def removeCgroup(cgroup):
    if cgroup:
        assert os.path.getsize(os.path.join(cgroup, 'tasks')) == 0
        try:
            os.rmdir(cgroup)
        except OSError:
            # sometimes this fails because the cgroup is still busy, we try again once
            try:
                os.rmdir(cgroup)
            except OSError as e:
                logging.warning("Failed to remove cgroup {0}: error {1} ({2})"
                                .format(cgroup, e.errno, e.strerror))