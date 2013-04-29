#!/usr/bin/python

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

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import os
import time
import argparse
import subprocess
import tempita
import json


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

    if code != 0:
        print ('Error: Could not call GraphViz to create graph {0} (error code {1})'.format(outfile, code))
        print ('Report generation failed')
        sys.exit(1)

    return True


def readfile(filepath, optional=False):
    if not os.path.isfile(filepath):
        if optional:
            return None
        raise Exception('File not found: ' + filepath)
    #print ('Reading: ' + filepath)
    with open(filepath, 'r') as fp:
        return fp.read()


def main():

    parser = argparse.ArgumentParser(
        description="Generate a HTML report with graphs from the CPAchecker output."
    )
    parser.add_argument("-r", "--reportpath",
        dest="reportdir",
        help="Directory for report (default: CPAchecker output path)"
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
    with open(options.configfile) as configfile:
        for line in configfile:
            (key, val) = line.split("=", 1)
            config[key.strip()] = val.strip()

    # extract paths to all necessary files from config
    cpaoutdir      = config.get('output.path', 'output/')
    sourcefile     = config.get('analysis.programNames')
    logfile        = os.path.join(cpaoutdir, config.get('log.file', 'CPALog.txt'))
    statsfile      = os.path.join(cpaoutdir, config.get('statistics.file', 'Statistics.txt'))
    argfilepath    = os.path.join(cpaoutdir, config.get('cpa.arg.file', 'ARG.dot'))
    errorpathgraph = os.path.join(cpaoutdir, config.get('cpa.arg.errorPath.graph', 'ErrorPath.dot'))
    errorpath      = os.path.join(cpaoutdir, config.get('cpa.arg.errorPath.json', 'ErrorPath.json'))
    combinednodes  = os.path.join(cpaoutdir, 'combinednodes.json')
    cfainfo        = os.path.join(cpaoutdir, 'cfainfo.json')
    fcalledges     = os.path.join(cpaoutdir, 'fcalledges.json')

    scriptdir   = os.path.dirname(__file__)
    reportdir   = options.reportdir or cpaoutdir
    tplfilepath = os.path.join(scriptdir, 'report-template.html')
    outfilepath = os.path.join(reportdir, 'index.html')

    time_generated = time.strftime("%a, %d %b %Y %H:%M", time.localtime())

    if not os.path.isdir(reportdir):
        os.makedirs(reportdir)

    #if there is an ARG.dot create an SVG in the report dir
    if os.path.isfile(argfilepath):
        print ('Generating SVG for ARG (press Ctrl+C if this takes too long)')
        if not call_dot(argfilepath, reportdir) and os.path.isfile(errorpathgraph):
            if call_dot(errorpathgraph, reportdir):
                os.rename(os.path.join(reportdir, 'ErrorPath.svg'),
                          os.path.join(reportdir, 'ARG.svg'))

    print ('Generating SVGs for CFA')
    functions = [x[5:-4] for x in os.listdir(cpaoutdir) if x.startswith('cfa__') and x.endswith('.dot')]
    for func in functions:
        call_dot(os.path.join(cpaoutdir, 'cfa__' + func + '.dot'), reportdir)

    template = tempita.HTMLTemplate.from_filename(tplfilepath, encoding='UTF-8')

    # write file
    with open(outfilepath, 'w') as outf:
        outf.write(template.substitute(
            time_generated   =time_generated,
            sourcefilename   =os.path.basename(sourcefile),

            sourcefilecontent=readfile(sourcefile, optional=True),
            logfile          =readfile(logfile, optional=True),
            statistics       =readfile(statsfile, optional=True),
            conffile         =readfile(options.configfile, optional=True),

            # JSON data for script
            errorpath        =readfile(errorpath, optional=True) or '[]',
            functionlist     =json.dumps(functions),
            combinednodes    =readfile(combinednodes),
            cfainfo          =readfile(cfainfo),
            fcalledges       =readfile(fcalledges),
        ))

    print ('Report generated in {0}'.format(outfilepath))

    try:
        with open(os.devnull, 'w') as devnull:
            subprocess.Popen(['xdg-open', outfilepath],
                             stdout=devnull, stderr=devnull)
    except OSError:
        pass


if __name__ == '__main__':
    main()
