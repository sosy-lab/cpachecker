#!/bin/bash
#/home/yinboyu/workspace/codebase/csurf-results/OAI-UE
#/home/yinboyu/workspace/CFAtest/lock

export JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
csurf -nogui /home/yinboyu/workspace/CFAtest/lock -l /home/yinboyu/workspace/cpachecker/scripts/csurfJava_plugin.stk
