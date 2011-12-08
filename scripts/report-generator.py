#!/usr/bin/python

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2011  Dirk Beyer
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

import os
import re
import sys
import time
import shutil
import optparse
import subprocess
try:
    import json
except ImportError:
    #for python 2.5 and below, hope that simplejson is available
    import simplejson as json

SUBSTITUTION_RE = re.compile('\{\{\{(\w+)\}\}\}')

class Template(object):
    """
    a pretty limited template "engine" :)
    """

    def __init__(self, infile, outfile):
        self.infile = infile
        self.outfile = outfile

    def render(self, **kws):
        """
        pass kwargs here. The value should be a callable that returns the string to insert for the key.

        render(test=lambda : "This is a test!!") will substitute {{{test}}} for "This is a test!!"

        WARNING: currently only one template var e.g. {{{test}}} may appear on a single line in the template.

        """
        for line in self.infile:
            match = SUBSTITUTION_RE.search(line)
            if match and match.group(1) in kws:
                subst = kws[match.group(1)]()
                self.outfile.write(line[:match.start()])
                self.outfile.write(subst)
                self.outfile.write(line[match.end():])
            else:
                self.outfile.write(line)

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

    if code != 0:
        print ('Error: Could not call GraphViz to create graph {0} (error code {1})'.format(outfile, code))
        print ('Report generation failed')
        sys.exit(1)

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
    parser.add_option("-a", "--art",
        action="store",
        type="string",
        dest="art",
        help="CPAChecker ART.file"
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
        help="CPAChecker cpa.art.errorPath.json"
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
    artfilepath = options.art or os.path.join(cpaoutdir, 'ART.dot')
    errorpath = options.errorpath or os.path.join(cpaoutdir, 'ErrorPath.json')
    combinednodes = os.path.join(reportdir, 'combinednodes.json')
    cfainfo = os.path.join(reportdir, 'cfainfo.json')
    fcalledges = os.path.join(reportdir, 'fcalledges.json')
    logfile = options.logfile or os.path.join(cpaoutdir, 'CPALog.txt')
    statsfile = options.statsfile or os.path.join(cpaoutdir, 'Statistics.txt')
    conffile = options.conffile or os.path.join(cpacheckerdir, 'config', 'predicateAnalysis.properties')
    cilfile = args[0]
    time_generated = time.strftime("%a, %d %b %Y %H:%M", time.localtime())

    def filecontents(filepath, encode=False, fallback=None):
        def inner():
            if not os.path.isfile(filepath):
                if fallback:
                    return fallback
                if not encode:
                    raise Exception('File not found: ' + filepath)
                else:
                    return 'Not found:' + filepath
            print ('Reading: ' + filepath)
            with open(filepath, 'r') as fp:
                if encode:
                    return fp.read().replace('<','&lt;').replace('>', '&gt;')
                else:
                    return fp.read()
        return inner

    def gen_functionlist():
        funclist = [x[5:-4] for x in os.listdir(cpaoutdir) if x.startswith('cfa__') and x.endswith('.dot')]
        print ('Generating SVGs for CFA')
        for func in funclist:
            call_dot(os.path.join(cpaoutdir, 'cfa__' + func + '.dot'), reportdir)
        return json.dumps(funclist, indent=4)

    def format_cil():
        if not os.path.isfile(cilfile):
            return '<h3>CIL file not found.</h3>'
        else:
            with open(cilfile, 'r') as fp:
                buff = ['<table id="cil_holder">']
                for no, line in enumerate(fp):
                    buff.append('<tr id="cil_line_%d"><td><pre>%d</pre></td><td><pre>%s</pre></td></tr>\n' % (
                            no,
                            no,
                            line.replace('<','&lt;').replace('>', '&gt;').rstrip()
                        )
                    )
                buff.append('</table>')
                return ''.join(buff)

    #if there is an ART.dot create an SVG in the report dir
    if os.path.isfile(artfilepath):
        print ('Generating SVG for ART')
        call_dot(artfilepath, reportdir)

    inf = open(tplfilepath, 'r')
    outf = open(outfilepath, 'w')

    t = Template(inf, outf)
    t.render(
        logfile=filecontents(logfile, encode=True),
        statistics=filecontents(statsfile, encode=True),
        conffile=filecontents(conffile, encode=True),
        errorpath=filecontents(errorpath, fallback='[]'),
        functionlist=gen_functionlist,
        combinednodes=filecontents(combinednodes),
        cfainfo=filecontents(cfainfo),
        fcalledges=filecontents(fcalledges),
        time_generated=lambda: time_generated,
        formatted_cil=format_cil,
        sourcefile=lambda: cilfile[cilfile.rfind('/') + 1:], # get filename without path
    )

    inf.close()
    outf.close()
    print ('Report generated in {0}'.format(outfilepath))

if __name__ == '__main__':
    main()
