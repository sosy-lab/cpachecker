LTE model checker based on CodeSurfer and CPAchecker
===============================

CodeSurfer
----------------------------
[`CodeSurfer`](https://www.grammatech.com/products/codesurfer) is a code browser that understands pointers, indirect function calls, and whole-program effects.

CPAchecker
----------------------------

For information on how to install CPAchecker, see [`INSTALL.md`](INSTALL.md), and how to run it, see [`RUN.md`](RUN.md).

LTEScope
----------------------------

For debugging: 

1. using `JAVA_TOOL_OPTIONS` to augment the command line for remote debugging. 

    export JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

2. set debugging point in codes

3. run ['script/runJavaPlugin.sh'](script/runJavaPlugin.sh)

4. with Intellij or Eclipse, build a remote debug configuration. The `host` address depends on where the plugin runs.