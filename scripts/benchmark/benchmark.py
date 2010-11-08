#!/usr/bin/env python

from datetime import datetime
from string import Template
from xml.etree.ElementTree import ElementTree

import glob
import itertools
import json
import logging
import os
import resource
import shlex
import signal
import subprocess
import sys
import xml.etree.ElementTree as ET

class Benchmark:
    pass

class Test:
    pass

class Kill(Exception):
    pass

def kill_handler(signum, frame):
    raise Kill()

def run(args, rlimits):
    def setrlimits():
        for rsrc, limits in rlimits.items():
            resource.setrlimit(rsrc, limits)
    ru_before = resource.getrusage(resource.RUSAGE_CHILDREN)
    p = subprocess.Popen(args,
                         stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                         preexec_fn=setrlimits)
    try:
        (stdoutdata, stderrdata) = p.communicate()
    except Kill:
        logging.debug("sigkill!")
    ru_after = resource.getrusage(resource.RUSAGE_CHILDREN)
    timedelta = (ru_after.ru_utime + ru_after.ru_stime)\
        - (ru_before.ru_utime + ru_before.ru_stime)
    return (stdoutdata, stderrdata, timedelta)

def run_cbmc(options, sourcefile, rlimits):
    assert "--xml-ui" in options
    args = ["cbmc"] + options + [sourcefile]
    (stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    tree = ET.fromstring(stdoutdata)
    status = tree.findtext('cprover-status')
    return (status, timedelta)

def run_cpachecker(options, sourcefile, rlimits):
    args = ["cpachecker"] + options + [sourcefile]
    (stdoutdata, stderrdata, timedelta) = run_subprocess(args, rlimits)
    status = None
    for line in stdoutdata:
        if (line.find('java.lang.OutOfMemoryError') != -1) or\
                line.startswith('out of memory'):
            status = 'OUT OF MEMORY'
        elif line.find('SIGSEGV') != -1:
            status = 'SEGMENTATION FAULT'
        elif status is None or status == "ERROR (1)" and\
                line.find('Exception') != -1:
            status = 'EXCEPTION'
        elif status is None and line.startswith('Error location(s) reached?'):
            line = line[26:].strip()
            if line.startswith('NO'):
                status = 'SAFE'
            elif line.startswith('YES'):
                status = 'UNSAFE'
            else:
                status = 'UNKNOWN'
        if status is None and line.startswith('#Test cases computed:'):
            status = 'OK'
    return (status, timedelta)

def run_satabs(options, sourcefile, rlimits):
    args = ["satabs"] + options + [sourcefile]
    (stdoutdata, stderrdata, timedelta) = run_subprocess(args, rlimits)
    if "VERIFICATION SUCCESSFUL" in stdoutdata:
        status = "SUCCESS"
    else:
        status = "FAILURE"
    return (status, timedelta)

def load_benchmark(path):
    try:
        from xml.parsers.xmlproc  import xmlval
        validator = xmlval.XMLValidator()
        validator.parse_resource(path)
    except ImportError:
        logging.debug("I cannot import xmlval so I'm skipping the validation.")
        logging.debug("If you want xml validation please install pyxml.")
    tree = ElementTree()
    root = tree.parse(path)
    benchmark = Benchmark()
    benchmark.tool = root.get("tool")
    benchmark.tests = []
    for test in root.findall("test"):
        t = Test()
        t.sourcefiles = []
        for sourcefiles in test.findall("sourcefiles"):
            path = os.path.expandvars(os.path.expanduser(sourcefiles.text))
            t.sourcefiles += glob.glob(path)
        t.options = []
        for option in test.find("options").findall("option"):
            t.options.append(option.get("name"))
            if option.text is not None:
                t.options.append(option.text)
        benchmark.tests.append(t)
    return benchmark

def main(argv=None):
    if argv is None:
        argv = sys.argv
    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option("-d", "--debug",
                      action="store_true",
                      help="enable debug output")
    (options, args) = parser.parse_args(argv)
    if len(args) < 2:
        parser.error("invalid number of arguments")
    if (options.debug):
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s")
    rlimits = {}
    for i in xrange(1, len(args)):
        benchmark = load_benchmark(args[i])
        run_func = eval("run_" + benchmark.tool)
    #signal.signal(signal.SIGKILL, kill_handler)
        logging.debug("I'm benchmarking '{0}'.".format(args[i]))
        for test in benchmark.tests:
            for sourcefile in test.sourcefiles:
                logging.debug("I'm running 'cbmc {0} {1}'.".format(
                        " ".join(test.options), sourcefile))
                (status, timedelta) = run_func(test.options,
                                               sourcefile,
                                               rlimits)
                print ",".join([sourcefile, status, str(timedelta)])
    
if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        pass
