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
import optparse
import subprocess
import tempita
try:
    import json
except ImportError:
    #for python 2.5 and below, hope that simplejson is available
    import simplejson as json


def call_dot(infile, outpath):
    (basefilename, ext) = os.path.splitext(os.path.basename(infile))
    outfile = os.path.join(outpath, basefilename + '.svg')
    print ('   generating ' + infile)

    code = 0
    try:
        p = subprocess.Popen(['dot', '-Nfontsize=10', '-Efontsize=10',
                              '-Efontname=Courier New', '-Tsvg', '-o',
                              outfile, infile], stdout=subprocess.PIPE)
        code = p.wait()
    except KeyboardInterrupt: # strg + c
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
    print ('Reading: ' + filepath)
    with open(filepath, 'r') as fp:
        return fp.read()


def main():

    parser = optparse.OptionParser('%prog [options] sourcefile')
    parser.add_option("-r", "--reportpath",
        action="store",
        type="string",
        dest="reportdir",
        help="Directory for report"
    )
    parser.add_option("-o", "--outputpath",
        action="store",
        type="string",
        dest="outdir",
        help="CPAChecker output.path"
    )
    parser.add_option("-a", "--arg",
        action="store",
        type="string",
        dest="arg",
        help="CPAChecker ARG.file"
    )
    parser.add_option("-l", "--logfile",
        action="store",
        type="string",
        dest="logfile",
        help="CPAChecker log.file"
    )
    parser.add_option("-s", "--statistics",
        action="store",
        type="string",
        dest="statsfile",
        help="CPAChecker statistics.file"
    )
    parser.add_option("-e", "--errorpath",
        action="store",
        type="string",
        dest="errorpath",
        help="CPAChecker cpa.arg.errorPath.json"
    )
    parser.add_option("-g", "--errorpathgraph",
        action="store",
        type="string",
        dest="errorpathgraph",
        help="CPAChecker cpa.arg.errorPath.graph"
    )
    parser.add_option("-c", "--config",
        action="store",
        type="string",
        dest="conffile",
        help="path to CPAChecker config file"
    )

    options, args = parser.parse_args()
    if len(args) != 1:
         parser.error('Incorrect number of arguments, you need to specify the source code file')

    print ('Generating report')
    scriptdir = os.path.dirname(__file__)
    cpacheckerdir = os.path.normpath(os.path.join(scriptdir, '..'))
    cpaoutdir = options.outdir or os.path.join(cpacheckerdir, 'output')
    reportdir = options.reportdir or cpaoutdir
    tplfilepath = os.path.join(scriptdir, 'report-template.html')
    outfilepath = os.path.join(reportdir, 'index.html')
    argfilepath = options.arg or os.path.join(cpaoutdir, 'ARG.dot')
    errorpathgraph = options.errorpathgraph or os.path.join(cpaoutdir, 'ErrorPath.dot')
    errorpath = options.errorpath or os.path.join(cpaoutdir, 'ErrorPath.json')
    combinednodes = os.path.join(reportdir, 'combinednodes.json')
    cfainfo = os.path.join(reportdir, 'cfainfo.json')
    fcalledges = os.path.join(reportdir, 'fcalledges.json')
    logfile = options.logfile or os.path.join(cpaoutdir, 'CPALog.txt')
    statsfile = options.statsfile or os.path.join(cpaoutdir, 'Statistics.txt')
    conffile = options.conffile or os.path.join(cpacheckerdir, 'config', 'predicateAnalysis.properties')
    sourcefile = args[0]
    time_generated = time.strftime("%a, %d %b %Y %H:%M", time.localtime())

    #if there is an ARG.dot create an SVG in the report dir
    if os.path.isfile(argfilepath):
        print ('Generating SVG for ARG')
        if not call_dot(argfilepath, reportdir) and os.path.isfile(errorpathgraph):
            if call_dot(errorpathgraph, reportdir):
                os.rename(os.path.join(reportdir, 'ErrorPath.svg'),
                          os.path.join(reportdir, 'ARG.svg'))

    functions = [x[5:-4] for x in os.listdir(cpaoutdir) if x.startswith('cfa__') and x.endswith('.dot')]
    print ('Generating SVGs for CFA')
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
            conffile         =readfile(conffile, optional=True),

            # JSON data for script
            errorpath        =readfile(errorpath, optional=True) or '[]',
            functionlist     =json.dumps(functions),
            combinednodes    =readfile(combinednodes),
            cfainfo          =readfile(cfainfo),
            fcalledges       =readfile(fcalledges),
        ))

    print ('Report generated in {0}'.format(outfilepath))

if __name__ == '__main__':
    main()
