
import subprocess

import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):
    """
    This class serves as tool adaptor for ESBMC (http://www.esbmc.org/)
    """

    def getExecutable(self):
        return Util.findExecutable('lib/native/x86_64-linux/esbmc_64_static')


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()


    def getName(self):
        return 'ESBMC'


    def getCmdline(self, executable, options, sourcefile):
        return [executable] + options + [sourcefile]


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = 'UNKNOWN'
        
        if self.allInText(['Violated property:',
                      'dereference failure: dynamic object lower bound',
                      'VERIFICATION FAILED'],
                      output):
            status = 'FALSE(valid-deref)'
        elif self.allInText(['Violated property:',
                      'Operand of free must have zero pointer offset',
                      'VERIFICATION FAILED'],
                      output):
            status = 'FALSE(valid-free)'
        elif self.allInText(['Violated property:',
                      'error label',
                      'VERIFICATION FAILED'],
                      output):
            status = 'UNSAFE (error label)'
        elif self.allInText(['Violated property:',
                      'assertion',
                      'VERIFICATION FAILED'],
                      output):
            status = 'UNSAFE (assertion)'
        elif self.allInText(['Violated property:',
                      'dereference failure: forgotten memory',
                      'VERIFICATION FAILED'],
                      output):
            status = 'FALSE(valid-memtrack)'
        elif 'VERIFICATION SUCCESSFUL' in output:
            status = 'SAFE'

        if status == 'UNKNOWN' and output.endswith(('error', 'error\n')):
            status = 'ERROR'

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
