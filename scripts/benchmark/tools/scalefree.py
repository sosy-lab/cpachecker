
import benchexec.tools.template
import benchexec.result as result

class Tool(benchexec.tools.template.BaseTool):

    def name(self):
        return 'scalefree'

    def executable(self):
        return "sample-scalefree-setprop.sh"

    def determine_result(self, returncode, returnsignal, output, isTimeout):
        if returnsignal == 0 and returncode > 128:
            # shells sets return code to 128+signal when a signal is received
            returnsignal = returncode - 128

        if returnsignal != 0:
            if returnsignal == 6:
                status = 'ABORTED'
            elif ((returnsignal == 9) or (returnsignal == 15)) and isTimeout:
                status = 'TIMEOUT'
            elif returnsignal == 11:
                status = 'SEGMENTATION FAULT'
            elif returnsignal == 15:
                status = 'KILLED'
            else:
                status = 'KILLED BY SIGNAL '+str(returnsignal)

        elif returncode != 0:
            status = 'ERROR ({0})'.format(returncode)

        else:

            for line in output:
                if "UNSAT" in line:
                    status = "FALSE"
                elif "SAT" in line:
                    status = "TRUE"

        if not status:
            status = result.RESULT_UNKNOWN
        return status
