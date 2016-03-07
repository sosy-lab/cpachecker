#!/usr/bin/env python3

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

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import glob
import os
import argparse
import subprocess
import signal

for egg in glob.glob(os.path.join(os.path.dirname(__file__), os.pardir, 'lib', 'python-benchmark', '*.egg')):
    sys.path.insert(0, egg)

def call_dot(infile, outpath):
    (basefilename, ext) = os.path.splitext(os.path.basename(infile))
    outfile = os.path.join(outpath, basefilename + '.svg')
    #print ('   generating ' + infile)

    code = 0
    try:
        p = subprocess.Popen(['dot', '-Nfontsize=10', '-Efontsize=10',
                              '-Efontname=Courier New', '-Tsvg', '-o',
                              outfile, infile])
        code = p.wait()
    except KeyboardInterrupt: # ctrl + c
        print ('   skipping ' + infile)
        p.terminate()

        try:
            os.remove(outfile) # sometimes outfile is written half, so cleanup
        except OSError:
            pass # if outfile is not written, removing is impossible
        return False
    except OSError as e:
        if e.errno == 2:
            sys.exit('Error: Could not call "dot" from GraphViz to create graph, please install it\n({}).'.format(e))
        else:
            sys.exit('Error: Could not call "dot" from GraphViz to create graph\n({}).'.format(e))


    if code != 0:
        sys.exit('Error: Could not call "dot" from to create graph {0} (return code {1}).'.format(outfile, code))

    return True


def generateReport(cpaoutdir, functions, argfilepath, outfilepath, tplfilepath):
    fin = open(tplfilepath, 'r')
    fout = open(outfilepath, 'w')
    for line in fin:
        if 'CFAFUNCTIONGRAPHS' in line:
            writeCFA(cpaoutdir, functions, fout)
        elif 'ARGGRAPHS' in line:
            writeARG(argfilepath, fout)
        else:
            fout.write(line)

    print ('Report generated in {0}'.format(outfilepath))

    try:
        with open(os.devnull, 'w') as devnull:
            subprocess.Popen(['xdg-open', outfilepath],
                             stdout=devnull, stderr=devnull)
    except OSError:
        pass
    fin.close()
    fout.close()

def writeCFA(cpaoutdir, functions, outf):
    i = 0 
    for func in functions:
        start = False
        cfafile = open(os.path.join(cpaoutdir, 'cfa__' + func + '.svg'))
        for line in cfafile:
            if start:
                line = line.replace('class="node"','class="node" ng-dblclick="clickedCFAElement($event)"')
                line = line.replace('class="edge"','class="edge" ng-dblclick="clickedCFAElement($event)"')
                outf.write(line)
            if '<svg' in line:
                outf.write(line[:5] + " ng-show = \"cfaFunctionIsSet(" + str(i) + ")\" " + line[5:])
                start = True
        i = i+1
    cfafile.close()

def writeARG(argfilepath, outf):
    start = False
    argfile = open(argfilepath[:-4] + '.svg')
    for line in argfile:
        if '<svg' in line:
            start = True
        if start:
            line = line.replace('class="node"','class="node" ng-dblclick="clickedARGElement($event)"')
            line = line.replace('class="edge"','class="edge" ng-dblclick="clickedARGElement($event)"')
            outf.write(line)
    argfile.close()

def signal_handler(signal, frame):
    print("Received a keyboard interrupt. Exiting.")
    sys.exit(0)

def main():
    signal.signal(signal.SIGINT, signal_handler)

    parser = argparse.ArgumentParser(
        description="Generate a HTML report with graphs from the CPAchecker output."
    )
    parser.add_argument("-c", "--config",
        dest="configfile",
        default="output/UsedConfiguration.properties",
        help="""File with all the used CPAchecker configuration files
             (default: output/UsedConfiguration.properties)"""
    )

    options = parser.parse_args()

    print ('Generating report')

    # read config file
    config = {}
    try:
        with open(options.configfile) as configfile:
            for line in configfile:
                (key, val) = line.split("=", 1)
                config[key.strip()] = val.strip()
    except IOError as e:
        if e.errno:
            sys.exit('Could not find output of CPAchecker in {}. Please specify correctpath with option --config\n({}).'.format(options.configfile, e))
        else:
            sys.exit('Could not read output of CPAchecker in {}\n({}).'.format(options.configfile, e))

    if not config.get('analysis.programNames'):
        sys.exit('CPAchecker output does not specify path to analyzed program. Cannot generate report.')

    # extract paths to all necessary files from config
    cpaoutdir      = config.get('output.path', 'output/')
    argfilepath    = os.path.join(cpaoutdir, config.get('cpa.arg.file', 'ARG.dot'))
    errorpath      = os.path.join(cpaoutdir, config.get('cpa.arg.errorPath.json', 'ErrorPath.%d.json'))

    countexdir = "output/report"


    #if there is an ARG.dot create an SVG in the report dir
    if os.path.isfile(argfilepath):
        print ('Generating SVG for ARG (press Ctrl+C if this takes too long)')
        call_dot(argfilepath, cpaoutdir)

    print ('Generating SVGs for CFA')
    functions = [x[5:-4] for x in os.listdir(cpaoutdir) if x.startswith('cfa__') and x.endswith('.dot')]
    errorpathcount = len(glob.glob(errorpath.replace('%d', '*')))
    functions = sorted(functions)
    for func in functions:
        call_dot(os.path.join(cpaoutdir, 'cfa__' + func + '.dot'), cpaoutdir)

    if errorpathcount != 0:
      for index in range(errorpathcount):
        outfilepath = os.path.join(countexdir, 'report_' + str(index) + '.html')
        tplfilepath = os.path.join(countexdir, 'report_withoutGraphs_' + str(index) + '.html')
        generateReport(cpaoutdir, functions, argfilepath, outfilepath, tplfilepath)
    else:
      outfilepath = os.path.join(countexdir, 'report.html')
      tplfilepath = os.path.join(countexdir, 'report_withoutGraphs.html')
      generateReport(cpaoutdir, functions, argfilepath, outfilepath, tplfilepath)

if __name__ == '__main__':
    main()
