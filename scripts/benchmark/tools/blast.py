import subprocess
import os

import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('pblast.opt')


    def getProgrammFiles(self, executable):
        executableDir = os.path.join(os.path.dirname(executable))
        return [os.path.join(executableDir)]


    def getWorkingDirectory(self, executable):
        return os.curdir


    def getVersion(self, executable):
        return subprocess.Popen([executable],
                                stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT).communicate()[0][6:11]


    def getCmdline(self, blastExe, options, sourcefile):
        ocamlExe = Util.findExecutable('ocamltune')
        return [ocamlExe, blastExe] + options + [sourcefile]


    def getName(self):
        return 'BLAST'


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = 'UNKNOWN'
        for line in output.splitlines():
            if line.startswith('Error found! The system is unsafe :-('):
                status = 'UNSAFE'
            elif line.startswith('No error found.  The system is safe :-)'):
                status = 'SAFE'
            elif line.startswith('Fatal error: exception Out_of_memory'):
                status = 'OUT OF MEMORY'
            elif line.startswith('Error: label \'ERROR\' appears multiple times'):
                status = 'ERROR'
            elif (returnsignal == 9):
                status = 'TIMEOUT'
            elif 'Ack! The gremlins again!' in line:
                status = 'EXCEPTION (Gremlins)'
        return status