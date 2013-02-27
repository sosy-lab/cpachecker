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
    
    def getExecutable(self):
        return Util.findExecutable('lib/native/x86_64-linux/evolcheck')


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()

    def getName(self):
        return 'eVolCheck'

    def preprocessSourcefile(self, sourcefile):
        gotoCcExecutable  = Util.findExecutable('lib/native/x86_64-linux/goto-cc')
        gotoCcFilename    = sourcefile + ".cc"

        subprocess.Popen([gotoCcExecutable,
                            sourcefile,
                            '-o',
                            gotoCcFilename],
                          stdout=subprocess.PIPE).wait()
            
        return gotoCcFilename
            

    def getCmdline(self, executable, options, sourcefile):
        sourcefile = self.preprocessSourcefile(sourcefile)

        if ("--init-upgrade-check" in options):
            subprocess.check_call(["rm -f", "__summaries", "__omega"], shell=True)
            return [executable] + options + [sourcefile]

        else:
            return [executable] + ['--do-upgrade-check'] + [sourcefile] + options

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
