#!/usr/bin/env python

from datetime import datetime
from string import Template
from xml.etree.ElementTree import ElementTree

import glob
import itertools
import logging
import os
import resource
import subprocess
import sys
import xml.etree.ElementTree as ET

class Benchmark:
    pass

class Test:
    pass

def run(args, rlimits):
    args = map(lambda arg: os.path.expandvars(arg), args)
    args = map(lambda arg: os.path.expanduser(arg), args)
    def setrlimits():
        for rsrc, limits in rlimits.items():
            resource.setrlimit(rsrc, limits)
    ru_before = resource.getrusage(resource.RUSAGE_CHILDREN)
    try:
        p = subprocess.Popen(args,
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                             preexec_fn=setrlimits)
    except OSError:
        logging.critical("I caught an OSError. Assure that the directory containing the tool to be benchmarked is included in the PATH environment variable or an alias is set.")
        sys.exit("A critical exception caused me to exit non-gracefully. Bye.")
    (stdoutdata, stderrdata) = p.communicate()
    ru_after = resource.getrusage(resource.RUSAGE_CHILDREN)
    timedelta = (ru_after.ru_utime + ru_after.ru_stime)\
        - (ru_before.ru_utime + ru_before.ru_stime)
    returncode = p.returncode
    logging.debug("My subprocess returned returncode {0}.".format(returncode))
    return (returncode, stdoutdata, stderrdata, timedelta)

def run_cbmc(options, sourcefile, rlimits):
    assert "--xml-ui" in options
    args = ["cbmc"] + options + [sourcefile]
    (returncode, stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    tree = ET.fromstring(stdoutdata)
    status = tree.findtext('cprover-status')
    return (status, timedelta)

def run_cpachecker(options, sourcefile, rlimits):
    args = ["cpachecker"] + options + [sourcefile]
    (returncode, stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    if returncode == 0:
        status = None
    elif returncode == 134:
        status = "ABORTED (probably by Mathsat)"
    elif returncode == 137:
        status = "KILLED BY SIGNAL 9 (probably ulimit)"
    elif returncode == 143:
        status = "KILLED"
    else:
        status = "ERROR ({0})".format(returncode)
    for line in stdoutdata.splitlines():
        if (line.find('java.lang.OutOfMemoryError') != -1) or line.startswith('out of memory'):
            status = 'OUT OF MEMORY'
        elif line.find('SIGSEGV') != -1:
            status = 'SEGMENTATION FAULT'
        elif (status is None or status == "ERROR (1)") and line.find('Exception') != -1:
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
    if status is None:
        status = "UNKNOWN"
    return (status, timedelta)

def run_satabs(options, sourcefile, rlimits):
    args = ["satabs"] + options + [sourcefile]
    (returncode, stdoutdata, stderrdata, timedelta) = run(args, rlimits)
    if "VERIFICATION SUCCESSFUL" in stdoutdata:
        status = "SUCCESS"
    else:
        status = "FAILURE"
    return (status, timedelta)

def ordinal_numeral(number):
    last_cipher = number % 10
    if last_cipher == 1:
        return "{0}st".format(number)
    elif last_cipher == 2:
        return "{0}nd".format(number)
    elif last_cipher == 3:
        return "{0}rd".format(number)
    else:
        return "{0}th".format(number)

def load_benchmark(path):
    ## looks like trouble with pyxml, better use lxml (http://codespeak.net/lxml/).
    # try:
    #     from xml.parsers.xmlproc  import xmlval
    #     validator = xmlval.XMLValidator()
    #     validator.parse_resource(path)
    # except ImportError:
    #     logging.debug("I cannot import xmlval so I'm skipping the validation.")
    #     logging.debug("If you want xml validation please install pyxml.")
    benchmark_path = path
    logging.debug("I'm loading the benchmark {0}.".format(benchmark_path))
    tree = ElementTree()
    root = tree.parse(path)
    benchmark = Benchmark()
    benchmark.tool = root.get("tool")
    logging.debug("The tool to be benchmarked is {0}.".format(repr(benchmark.tool)))
    benchmark.tests = []
    for test in root.findall("test"):
        t = Test()
        t.sourcefiles = []
        sourcefiles_tags = test.findall("sourcefiles")
        for sourcefiles_tag in sourcefiles_tags:
            sourcefiles_path = os.path.expandvars(os.path.expanduser(sourcefiles_tag.text))
            if sourcefiles_path != sourcefiles_tag.text:
                logging.debug("I expanded a tilde and/or shell variables in expression {0} to {1}.".format(
                        repr(sourcefiles_tag.text), repr(sourcefiles_path))) 
            pathnames = glob.glob(sourcefiles_path)
            if len(pathnames) == 0:
                logging.debug("I found no pathnames matching {0}.".format(repr(sourcefiles_path)))
            else:
                t.sourcefiles += pathnames
        t.options = []
        for option in test.find("options").findall("option"):
            t.options.append(option.get("name"))
            if option.text is not None:
                t.options.append(option.text)
        benchmark.tests.append(t)
    testnum = len(benchmark.tests)
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
    for arg in args[1:]:
        if not os.path.exists(arg) or not os.path.isfile(arg):
            parser.error("File {0} does not exist.".format(repr(arg)))
    for arg in args[1:]:
        benchmark = load_benchmark(arg)
        run_func = eval("run_" + benchmark.tool)
        if len(benchmark.tests) == 1:
            logging.debug("I'm benchmarking {0} consisting of 1 test.".format(repr(arg)))
        else:
            logging.debug("I'm benchmarking {0} consisting of {1} tests.".format(
                    repr(arg), len(benchmark.tests)))
        for test in benchmark.tests:
            if len(test.sourcefiles) == 1:
                logging.debug("The {0} test consists of 1 sourcefile.".format(
                        ordinal_numeral(benchmark.tests.index(test) + 1)))
            else:
                logging.debug("The {0} test consists of {1} sourcefiles.".format(
                        ordinal_numeral(benchmark.tests.index(test) + 1),
                        len(test.sourcefiles)))
            for sourcefile in test.sourcefiles:
                logging.debug("I'm running '{0} {1} {2}'.".format(
                        benchmark.tool, " ".join(test.options), sourcefile))
                (status, timedelta) = run_func(test.options,
                                               sourcefile,
                                               rlimits)
                print ",".join([sourcefile, status, str(timedelta)])
        logging.debug("I think my job is done. Have a nice day!")

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        pass
