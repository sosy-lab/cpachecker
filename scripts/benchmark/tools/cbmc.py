import logging
import os
import platform
import subprocess
import xml.etree.ElementTree as ET

import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        fallback = "lib/native/x86_64-linux/cbmc" if platform.machine() == "x86_64" else \
                   "lib/native/x86-linux/cbmc"    if platform.machine() == "i386" else None
        return Util.findExecutable('cbmc', fallback)


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()


    def getName(self):
        return 'CBMC'


    def getCmdline(self, executable, options, sourcefiles, propertyfile):
        if ("--xml-ui" not in options):
            options = options + ["--xml-ui"]

        self.options = options

        return [executable] + options + sourcefiles


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        #an empty tag cannot be parsed into a tree
        output = output.replace("<>", "<emptyTag>")
        output = output.replace("</>", "</emptyTag>")

        if returnsignal == 0 and ((returncode == 0) or (returncode == 10)):
            try:
                tree = ET.fromstring(output)
                status = tree.findtext('cprover-status')

                if status is None:
                    def isErrorMessage(msg):
                        return msg.get('type', None) == 'ERROR'

                    messages = list(filter(isErrorMessage, tree.getiterator('message')))
                    if messages:
                        # for now, use only the first error message if there are several
                        msg = messages[0].findtext('text')
                        if msg == 'Out of memory':
                            status = 'OUT OF MEMORY'
                        elif msg:
                            status = 'ERROR (%s)'.format(msg)
                        else:
                            status = 'ERROR'
                    else:
                        status = 'INVALID OUTPUT'

                elif status == "FAILURE":
                    assert returncode == 10
                    reason = tree.find('goto_trace').find('failure').findtext('reason')
                    if 'unwinding assertion' in reason:
                        status = result.STATUS_UNKNOWN
                    else:
                        status = result.STATUS_FALSE_REACH

                elif status == "SUCCESS":
                    assert returncode == 0
                    if "--no-unwinding-assertions" in self.options:
                        status = result.STATUS_UNKNOWN
                    else:
                        status = result.STATUS_TRUE_PROP

            except Exception as e: # catch all exceptions
                if isTimeout:
                    # in this case an exception is expected as the XML is invalid
                    status = 'TIMEOUT'
                elif 'Minisat::OutOfMemoryException' in output:
                    status = 'OUT OF MEMORY'
                else:
                    status = 'INVALID OUTPUT'
                    logging.warning("Error parsing CBMC output for returncode %d: %s" % (returncode, e))

        elif returncode == 6:
            # parser error or something similar
            status = 'ERROR'

        elif returnsignal == 9:
            if isTimeout:
                status = 'TIMEOUT'
            else:
                status = "KILLED BY SIGNAL 9"

        elif returnsignal == 6:
            status = "ABORTED"
        elif returnsignal == 15 or returncode == 15:
            status = "KILLED"
        elif returncode == 64 and 'Usage error!' in output:
            status = 'INVALID ARGUMENTS'
        else:
            status = "ERROR ({0})".format(returncode)

        return status
