#!/usr/bin/python
from __future__ import with_statement #for python 2.5
import os
import re
import time
import shutil
import optparse
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
          
TRANSITION_REGEX = re.compile("Line ([0-9]+): \(N([0-9]+) -\{(.*?)\}-> N([0-9]+)\)\r?\n?")

def errorpath2list(errpathfile):
    print 'Converting errorpath to JSON'
    jsonpath = []
    if os.path.isfile(errpathfile):
        for transition in open(errpathfile):
            match = TRANSITION_REGEX.match(transition)
            if not match:
                print 'Warning: line did not match pattern: ' + transition
            else:
                jsonpath.append(
                    dict(
                        line=int(match.group(1)),
                        source=int(match.group(2)),
                        target=int(match.group(4)),
                        desc=match.group(3),
                    )
                )    
    return jsonpath

def call_dot(path, filename, out=None):
    (basefilename, ext) = os.path.splitext(filename)
    out = (out or basefilename) + '.svg'
    os.system('cd %s;dot -Nfontsize=10 -Efontsize=10 -Efontname="Courier New" -Tsvg -o %s %s' % (path, out, filename))

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
    
    print 'generating report'
    scriptdir = os.path.dirname(__file__)
    cpacheckerdir = os.path.normpath(os.path.join(scriptdir, '..'))
    cpaoutdir = options.outdir or os.path.join(cpacheckerdir, 'test', 'output')
    reportdir = options.reportdir or cpaoutdir
    tplfilepath = os.path.join(scriptdir, 'report_template.html')
    outfilepath = os.path.join(reportdir, 'index.html')
    artfilepath = options.art or os.path.join(cpaoutdir, 'ART.dot')
    errorpath = options.errorpath or os.path.join(cpaoutdir, 'ErrorPath.json')
    combinednodes = os.path.join(reportdir, 'combinednodes.json')
    cfainfo = os.path.join(reportdir, 'cfainfo.json')    
    fcalledges = os.path.join(reportdir, 'fcalledges.json')
    logfile = options.logfile or os.path.join(cpaoutdir, 'CPALog.txt')
    statsfile = options.statsfile or os.path.join(cpaoutdir, 'Statistics.txt')
    conffile = options.conffile or os.path.join(cpacheckerdir, 'test', 'config', 'symbpredabsCPA.properties')
    cilinput = args[0]
    time_generated = time.strftime("%a, %d %b %Y %H:%M", time.localtime())
    cilfile = os.path.join(reportdir, 'a.cil.c')
    
    
    #if there is an ART.dot create an SVG in the report dir
    if os.path.isfile(artfilepath):
        artfile = os.path.basename(artfilepath)
        artpath = os.path.dirname(artfilepath) or './'
        call_dot(artpath, artfile, os.path.join(reportdir, 'ART'))
    
    shutil.copy(cilinput, cilfile)
    
    inf = open(tplfilepath, 'r')
    outf = open(outfilepath, 'w')

    def filecontents(filepath, encode=False, fallback=None):
        def inner():
            if not os.path.isfile(filepath):
                if fallback:
                    return fallback
                if not encode:
                    raise Exception('File not found: ' + filepath)
                else:
                    return '<h2>Not found:' + filepath + '</h2>'
            print ' Reading: ' + filepath
            with open(filepath, 'r') as fp:
                if encode:
                    return fp.read().replace('<','&lt;').replace('>', '&gt;')
                else:
                    return fp.read()
        return inner

    def gen_errorpath():
        return json.dumps(errorpath2list(errorpath), indent=4)

    def gen_functionlist():
        print 'Generating the list of functions'
        funclist = [x[5:-4] for x in os.listdir(reportdir) if x.startswith('cfa__') and x.endswith('.dot')]
        print 'Generating SVGs for CFA'
        for func in funclist:
            call_dot(reportdir, 'cfa__' + func + '.dot')
        return json.dumps(funclist, indent=4)


    def gen_stats():
        return filecontents(statsfile, encode=True)

    def format_cil():
        if not os.path.isfile(cilfile):
            return '<h3>CIL file not found.</h3>'
        else:            
            with open(cilfile, 'r') as fp:
                buff = ['<table id="cil_holder">']
                for no, line in enumerate(fp):
                    buff.append('<tr id="cil_line_%d"><td><pre>%d</pre></td><td><pre>%s</pre></td></tr>' % (
                            no,
                            no,
                            line.replace('<','&lt;').replace('>', '&gt;').rstrip()
                        )
                    )
                buff.append('</table>')
                return ''.join(buff)
            
    t = Template(inf, outf)
    t.render(
        logfile=filecontents(logfile, encode=True),
        statistics=gen_stats(),
        conffile=filecontents(conffile, encode=True),
        #errorpath=gen_errorpath,
        errorpath=filecontents(errorpath, fallback='[]'),
        functionlist=gen_functionlist,
        combinednodes=filecontents(combinednodes),
        cfainfo=filecontents(cfainfo),
        fcalledges=filecontents(fcalledges),
        time_generated=lambda: time_generated,
        formatted_cil=format_cil,
    )
  
    inf.close()
    outf.close()

if __name__ == '__main__':
    main()
    
