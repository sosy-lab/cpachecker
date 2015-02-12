import subprocess

import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):

    def executable(self):
        return Util.find_executable('wolverine')


    def version(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].split()[1].strip()


    def name(self):
        return 'Wolverine'


    def determine_result(self, returncode, returnsignal, output, isTimeout):
        output = '\n'.join(output)
        if "VERIFICATION SUCCESSFUL" in output:
            assert returncode == 0
            status = result.STATUS_TRUE_PROP
        elif "VERIFICATION FAILED" in output:
            assert returncode == 10
            status = result.STATUS_FALSE_REACH
        elif returnsignal == 9:
            status = "TIMEOUT"
        elif returnsignal == 6 or (returncode == 6 and "Out of memory" in output):
            status = "OUT OF MEMORY"
        elif returncode == 6 and "PARSING ERROR" in output:
            status = "PARSING ERROR"
        else:
            status = "FAILURE"
        return status