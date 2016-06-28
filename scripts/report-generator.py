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
    (basefilename, _) = os.path.splitext(os.path.basename(infile))
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


def generate_report(cpa_output_dir, functions, arg_path, report_path):
    with open(report_path, 'r') as report:
        template = report.readlines()

    with open(report_path, 'w') as report:
        for line in template:
            if 'CFAFUNCTIONGRAPHS' in line:
                write_cfa(cpa_output_dir, functions, report)
            elif 'ARGGRAPHS' in line:
                write_arg(arg_path, report)
            else:
                report.write(line)

        print ('Report generated in {0}'.format(report_path))

    try:
        with open(os.devnull, 'w') as devnull:
            subprocess.Popen(['xdg-open', report_path], stdout=devnull, stderr=devnull)

    except OSError:
        pass

def write_cfa(cpa_output_dir, functions, report):
    i = 0
    for function in functions:
        start = False
        cfa_path = os.path.join(cpa_output_dir, 'cfa__' + function + '.svg')
        if os.path.exists(cfa_path):
            with open(cfa_path) as cfa_file:
                for line in cfa_file:
                    if start:
                        line = line.replace('class="node"','class="node" ng-dblclick="clickedCFAElement($event)"')
                        line = line.replace('class="edge"','class="edge" ng-dblclick="clickedCFAElement($event)"')
                        report.write(line)
                    if '<svg' in line:
                        report.write(line[:5] + " ng-show = \"cfaFunctionIsSet(" + str(i) + ")\" " + line[5:])
                        start = True
        i = i+1

def write_arg(arg_file_path, report):
    start = False
    arg_path = arg_file_path[:-4] + '.svg'
    if os.path.exists(arg_path):
        with open(arg_path) as argfile:
            for line in argfile:
                if '<svg' in line:
                    start = True
                if start:
                    line = line.replace('class="node"','class="node" ng-dblclick="clickedARGElement($event)"')
                    line = line.replace('class="edge"','class="edge" ng-dblclick="clickedARGElement($event)"')
                    report.write(line)

def parse_arguments():
    parser = argparse.ArgumentParser(description="Generate a HTML report with graphs from the CPAchecker output.")
    parser.add_argument("-c",
                        "--config",
                        dest="configfile",
                        default="output/UsedConfiguration.properties",
                        help="""File with all the used CPAchecker configuration files
                            (default: output/UsedConfiguration.properties)""")
    options = parser.parse_args()
    return options

def read_CPAchecker_config(options):
    # read config file
    config = {}

    try:
        with open(options.configfile) as configfile:
            for line in configfile:
                key, val = line.split("=", 1)
                config[key.strip()] = val.strip()
    except IOError as e:

        if e.errno:
            sys.exit('Could not find output of CPAchecker in {}. Please specify correctpath with option --config\n({}).'.format(options.configfile, e))
        else:
            sys.exit('Could not read output of CPAchecker in {}\n({}).'.format(options.configfile, e))
    if not config.get('analysis.programNames'):
        sys.exit('CPAchecker output does not specify path to analyzed program. Cannot generate report.')

    return config

def main():
    options = parse_arguments()
    config = read_CPAchecker_config(options)

    # extract paths to all necessary files and directories from config
    cpa_output_dir = config.get('output.path', 'output/')
    arg_path = os.path.join(cpa_output_dir, config.get('cpa.arg.file', 'ARG.dot'))
    counter_example_path_template = os.path.join(cpa_output_dir, config.get('counterexample.export.report', 'Counterexample.%d.html'))
    report_path = os.path.join(cpa_output_dir, config.get('report.file', 'Report.html'))

    counter_example_paths = glob.glob(counter_example_path_template.replace('%d', '*'))

    report_count = len(counter_example_paths)
    if os.path.exists(report_path):
        report_count = report_count + 1

    if report_count == 0:
        print("No reports found in " + cpa_output_dir)
        return

    print ('Generating', report_count, 'reports')

    #if there is an ARG.dot create an SVG in the report dir
    if os.path.isfile(arg_path):
        print ('Generating SVG for ARG (press Ctrl+C if this takes too long)')
        call_dot(arg_path, cpa_output_dir)

    print ('Generating SVGs for CFA:')
    functions = [x[5:-4] for x in os.listdir(cpa_output_dir) if x.startswith('cfa__') and x.endswith('.dot')]
    sorted_functions = sorted(functions)
    for function in sorted_functions:
        print("\t" + function)
        call_dot(os.path.join(cpa_output_dir, 'cfa__' + function + '.dot'), cpa_output_dir)

    for counter_example_path in counter_example_paths:
        generate_report(cpa_output_dir, sorted_functions, arg_path, counter_example_path)

    if os.path.exists(report_path):
        generate_report(cpa_output_dir, sorted_functions, arg_path, report_path)

if __name__ == '__main__':
    main()
