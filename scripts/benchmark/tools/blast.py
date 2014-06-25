import subprocess
import os

import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('pblast.opt')


    def getProgrammFiles(self, executable):
        executableDir = os.path.dirname(executable)
        return [executableDir]


    def getWorkingDirectory(self, executable):
        return os.curdir


    def getEnvironments(self, executable):
        executableDir = os.path.dirname(executable)
        workingDir = self.getWorkingDirectory(executable)
        return {"additionalEnv" : {'PATH' :  ':' + (os.path.relpath(executableDir, start=workingDir))}}


    def getVersion(self, executable):
        return subprocess.Popen([executable],
                                stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT).communicate()[0][6:11]


    def getCmdline(self, blastExe, options, sourcefiles, propertyfile):
        workingDir = self.getWorkingDirectory(blastExe)
        ocamlExe = Util.findExecutable('ocamltune')
        return [os.path.relpath(ocamlExe, start=workingDir), os.path.relpath(blastExe, start=workingDir)] + options + sourcefiles


    def getName(self):
        return 'BLAST'


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = result.STR_UNKNOWN
        for line in output.splitlines():
            if line.startswith('Error found! The system is unsafe :-('):
                status = result.STR_FALSE_REACH
            elif line.startswith('No error found.  The system is safe :-)'):
                status = result.STR_TRUE_PROP
            elif line.startswith('Fatal error: exception Out_of_memory'):
                status = 'OUT OF MEMORY'
            elif line.startswith('Error: label \'ERROR\' appears multiple times'):
                status = 'ERROR'
            elif (returnsignal == 9):
                status = 'TIMEOUT'
            elif 'Ack! The gremlins again!' in line:
                status = 'EXCEPTION (Gremlins)'
        return status
