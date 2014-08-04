
import subprocess
import os
import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):
    """
    This class serves as tool adaptor for ESBMC (http://www.esbmc.org/)
    """

    def getExecutable(self):
        return Util.findExecutable('esbmc')


    def getProgrammFiles(self, executable):
        executableDir = os.path.dirname(executable)
        return [executableDir]


    def getWorkingDirectory(self, executable):
        executableDir = os.path.dirname(executable)
        return executableDir


    def getEnvironments(self, executable):
        return {"additionalEnv" : {'PATH' :  ':.'}}


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()


    def getName(self):
        return 'ESBMC'


    def getCmdline(self, executable, options, sourcefiles, propertyfile, rlimits):
        assert len(sourcefiles) == 1, "only one sourcefile supported"
        sourcefile = sourcefiles[0]
        workingDir = self.getWorkingDirectory(executable)
        return [os.path.relpath(executable, start=workingDir)] + options + [os.path.relpath(sourcefile, start=workingDir)]



    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = result.STATUS_UNKNOWN

        if self.allInText(['Violated property:',
                      'dereference failure: dynamic object lower bound',
                      'VERIFICATION FAILED'],
                      output):
            status = result.STATUS_FALSE_DEREF
        elif self.allInText(['Violated property:',
                      'Operand of free must have zero pointer offset',
                      'VERIFICATION FAILED'],
                      output):
            status = result.STATUS_FALSE_FREE
        elif self.allInText(['Violated property:',
                      'error label',
                      'VERIFICATION FAILED'],
                      output):
            status = result.STATUS_FALSE_REACH
        elif self.allInText(['Violated property:',
                      'assertion',
                      'VERIFICATION FAILED'],
                      output):
            status = result.STATUS_FALSE_REACH
        elif self.allInText(['Violated property:',
                      'dereference failure: forgotten memory',
                      'VERIFICATION FAILED'],
                      output):
            status = result.STATUS_FALSE_MEMTRACK
        elif 'VERIFICATION SUCCESSFUL' in output:
            status = result.STATUS_TRUE_PROP

        if status == result.STATUS_UNKNOWN:
            if isTimeout:
                status = 'TIMEOUT'
            elif output.endswith(('Z3 Error 9', 'Z3 Error 9\n')):
                status = 'ERROR (Z3 Error 9)'
            elif output.endswith(('error', 'error\n')):
                status = 'ERROR'
            elif 'Encountered Z3 conversion error:' in output:
                status = 'ERROR (Z3 conversion error)'

        return status


    def addColumnValues(self, output, columns):
        """
        This method adds the values that the user requested to the column objects.
        If a value is not found, it should be set to '-'.
        If not supported, this method does not need to get overridden.
        """
        pass

    """ helper method """
    def allInText(self, words, text):
        """
        This function checks, if all the words appear in the given order in the text.
        """
        index = 0
        for word in words:
            index = text[index:].find(word)
            if index == -1:
                return False
        return True
