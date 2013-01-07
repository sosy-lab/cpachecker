import subprocess

import benchmark.util as Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('pblast.opt')

    @staticmethod
    def getVersion(executable):
        return subprocess.Popen([executable],
                                stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT).communicate()[0][6:9]

    @staticmethod
    def getName():
        return 'BLAST'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable] + options + [sourcefile]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        status = "UNKNOWN"
        for line in output.splitlines():
            if line.startswith('Error found! The system is unsafe :-('):
                status = 'UNSAFE'
            elif line.startswith('No error found.  The system is safe :-)'):
                status = 'SAFE'
            elif (returncode == 2) and line.startswith('Fatal error: out of memory.'):
                status = 'OUT OF MEMORY'
            elif (returncode == 2) and line.startswith('Fatal error: exception Sys_error("Broken pipe")'):
                status = 'EXCEPTION'
            elif (returncode == 2) and line.startswith('Ack! The gremlins again!: Sys_error("Broken pipe")'):
                status = 'TIMEOUT'
        return status

    @staticmethod
    def addColumnValues(output, columns):
        pass