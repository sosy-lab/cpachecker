# Windows compatibility for resource module
# This provides basic functionality for BenchExec on Windows

import time
import os

# Resource constants (from Unix resource module)
RUSAGE_SELF = 0
RUSAGE_CHILDREN = -1

# Resource usage structure
class ResourceUsage:
    def __init__(self):
        self.ru_utime = 0.0      # user time
        self.ru_stime = 0.0      # system time
        self.ru_maxrss = 0       # maximum resident set size
        self.ru_ixrss = 0        # integral shared memory size
        self.ru_idrss = 0        # integral unshared data size
        self.ru_isrss = 0        # integral unshared stack size
        self.ru_minflt = 0       # page reclaims
        self.ru_majflt = 0       # page faults
        self.ru_nswap = 0        # swaps
        self.ru_inblock = 0      # block input operations
        self.ru_oublock = 0      # block output operations
        self.ru_msgsnd = 0       # messages sent
        self.ru_msgrcv = 0       # messages received
        self.ru_nsignals = 0     # signals received
        self.ru_nvcsw = 0        # voluntary context switches
        self.ru_nivcsw = 0       # involuntary context switches

def getrusage(who):
    """Windows-compatible version of getrusage"""
    usage = ResourceUsage()
    # Windows doesn't provide detailed resource usage
    # Return minimal information
    return usage

def getrlimit(resource):
    """Windows-compatible version of getrlimit"""
    # Return unlimited for all resources
    return (-1, -1)

def setrlimit(resource, limits):
    """Windows-compatible version of setrlimit"""
    # Do nothing on Windows
    pass

# Resource limit constants
RLIMIT_CPU = 0
RLIMIT_FSIZE = 1
RLIMIT_DATA = 2
RLIMIT_STACK = 3
RLIMIT_CORE = 4
RLIMIT_RSS = 5
RLIMIT_NPROC = 6
RLIMIT_NOFILE = 7
RLIMIT_OFILE = RLIMIT_NOFILE
RLIMIT_MEMLOCK = 8
RLIMIT_VMEM = 9
RLIMIT_AS = RLIMIT_VMEM

RLIM_INFINITY = -1



