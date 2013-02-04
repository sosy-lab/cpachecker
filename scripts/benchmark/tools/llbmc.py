
import subprocess

import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):
    """
    This class serves as tool adaptor for LLBMC
    """

    def getExecutable(self):
        return Util.findExecutable('lib/native/x86_64-linux/llbmc')


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].splitlines()[2][8:18]


    def getName(self):
        return 'LLBMC'


    def getCmdline(self, executable, options, sourcefile):
        return [executable] + options + [sourcefile]


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = 'UNKNOWN'
        
        for line in output.splitlines():
            if 'FALSE(p_valid-deref)' in line:
                status = 'FALSE(valid-deref)'
            elif 'FALSE(p_valid-free)' in line:
                status = 'FALSE(valid-free)'
            elif 'FALSE(p_valid-memtrack)' in line:
                status = 'FALSE(valid-memtrack)'
            elif 'UNSAFE' in line:
                status = 'UNSAFE'
            elif 'SAFE' in line or 'TRUE' in line:
                status = 'SAFE'
            elif 'Assertion' in line and 'failed' in line:
                status = 'Assertion'

        return status


    def addColumnValues(self, output, columns):
        """
        This method adds the values that the user requested to the column objects.
        If a value is not found, it should be set to '-'.
        If not supported, this method does not need to get overridden.
        """
        pass
