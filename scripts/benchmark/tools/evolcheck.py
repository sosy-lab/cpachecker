import logging
import os
import platform
import tempfile
import subprocess
import hashlib
import xml.etree.ElementTree as ET

import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):
    
    lastSourcefile = None
    gotosourcefileDict = {}

    def getExecutable(self):
        return Util.findExecutable('evolcheck')


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()


    def getName(self):
        return 'eVolCheck'

    def filehash(self, filepath):
        sha1 = hashlib.sha1()
        f = open(filepath, 'rb')
        try:
            sha1.update(f.read())
        finally:
            f.close()
        return sha1.hexdigest()

    
    def preprocessSourcefile(self, sourcefile):
        if not sourcefile in Tool.gotosourcefileDict:
            sourcefileHash = self.filehash(sourcefile)
            gotosourcefile = '/tmp/{}.goto'.format(sourcefileHash)
        
            if not os.path.isfile(gotosourcefile):
                gotoccExecutable = Util.findExecutable('make-goto.sh')
                subprocess.check_call([gotoccExecutable, sourcefile, gotosourcefile])
        
            Tool.gotosourcefileDict[sourcefile] = gotosourcefile
            
        return Tool.gotosourcefileDict[sourcefile]
            

    def getCmdline(self, executable, options, sourcefile):
        dirNameSourcefile = os.path.dirname(sourcefile)
        if not Tool.lastSourcefile is None:
            dirNameLastSourcefile = os.path.dirname(Tool.lastSourcefile)
        else:
            dirNameLastSourcefile = None

        gotosourcefile = self.preprocessSourcefile(sourcefile)

        if dirNameSourcefile != dirNameLastSourcefile:
            Tool.lastSourcefile = None

        if Tool.lastSourcefile is None:
            args = options + ["--init-upgrade-check", gotosourcefile]
            subprocess.check_call(["rm -f", "__summaries", "__omega"], shell=True)
        else:
            lastGotosourcefile = self.preprocessSourcefile(Tool.lastSourcefile)
            args = options + ["--do-upgrade-check", gotosourcefile, lastGotosourcefile]

        self.options = args

        Tool.lastSourcefile = sourcefile
        return [executable] + args


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = None

        assertionHoldsFound = False
        verificationSuccessfulFound = False
        verificationFailedFound = False

        for line in output.splitlines():
            if 'A real bug found.' in line:
                status = 'UNSAFE'
            elif 'VERIFICATION SUCCESSFUL' in line:
                verificationSuccessfulFound = True
            elif 'VERIFICATION FAILED' in line:
                verificationFailedFound = True
            elif 'ASSERTION(S) HOLD(S)' in line:
                assertionHoldsFound = True

        if status is None:
            if verificationSuccessfulFound and not verificationFailedFound:
                status = 'SAFE'
            else:
                status = 'UNKNOWN'

        return status
