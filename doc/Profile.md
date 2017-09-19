Profiling
=========

There exist many tools that allow to monitor Java processes, e.g. VisualVM.
Most of these connect to the JVM via a special file in `/tmp/hsperfdata_*`.
CPAchecker when started with `scripts/cpa.sh`
disables this file by default for performance reasons
(cf. [http://www.evanjones.ca/jvm-mmap-pause.html](http://www.evanjones.ca/jvm-mmap-pause.html)),
so these tools won't see the CPAchecker process.
Just run CPAchecker with the environment variable
`JAVA_VM_ARGUMENTS=-XX:-PerfDisableSharedMem` set to enable this again.


Time profiling
--------------

CPAchecker has internal time statistics, which are dumped into Statistics.txt.
With -stats or statistics.print=true they are printed to the console, too.

Hprof can profile CPU usage:

1. Set the option `-agentlib:hprof=cpu=times` for the Java VM.
   1. When running from Eclipse: Insert the option into the box
      "VM arguments"  of the run configuration.
   2. When running from command line: Set the environment variable
      `JAVA_VM_ARGUMENTS` to this value by executing
      `export JAVA_VM_ARGUMENTS="-javaagent:-agentlib:hprof=cpu=times"`
2. Run CPAchecker.
3. Open the text file `java*.hprof.txt` and look at the times there.
   Unfortunately this is hard to read and does not give a nice, browsable,
   tree-like overview of the methods and the call tree.
   
Documentation: http://docs.oracle.com/javase/8/docs/technotes/samples/hprof.html

VisualVM can also profile CPU usage, but only with sampling.
It can only connect to a running process, so profiling of startup is not possible.

1. Start VisualVM (`jvisualvm` from Ubuntu package `visualvm`).
2. Run CPAchecker.
3. In VisualVM, connect to CPAchecker process, switch to tab `Sampler`
   and press `CPU` button.
4. Settings (for example sampling frequency and method filter)
   can be configured after setting the `Settings` checkbox in the upper right corner.
   
Documentation:

 - http://visualvm.java.net/profiler.html
 - https://blogs.oracle.com/nbprofiler/entry/profiling_with_visualvm_part_1
 - https://blogs.oracle.com/nbprofiler/entry/profiling_with_visualvm_part_2


Memory profiling
----------------

For graphing memory consumption over time:

1. Start VisualVM (`jvisualvm` from Ubuntu package `visualvm`).
2. In "Tools" -> "Plugins", install "Visual GC" plugin.
3. Run CPAchecker.
4. In VisualVM, connect to CPAchecker process,
   and look at "Monitor" and "Visual GC" tabs.

For viewing heap statistics (object count and memory usage per type):

1. Run `jmap -histo <PID>` where `<PID>` is the process ID of the running
   java process. This prints statistics of a current snapshot of the heap.
   
Documentation: https://docs.oracle.com/javase/8/docs/technotes/tools/windows/jmap.html

For viewing heap statistics for a complete CPAchecker run:

1. Set the option `-agentlib:hprof=heap=sites,depth=0` for the Java VM
   as described above.
2. Run CPAchecker.
3. Open the file `java.hprof.txt` in the CPAchecker directory.

Documentation: http://docs.oracle.com/javase/8/docs/technotes/samples/hprof.html

For viewing detailed heap content after an `OutOfMemoryError`:

1. Set the option `-XX:+HeapDumpOnOutOfMemoryError` for the Java VM
   as described above.
2. Run CPAchecker and wait for `OutOfMemoryError`. A file `java_pid<PID>.hprof`
   is produced in the CPAchecker directory.
3. Analyze the heap dump with one of the following tools:
   - VisualVM (Package visualvm)
     Run "visualvm" and open the produced file.
     Documentation: http://visualvm.java.net/heapdump.html, http://visualvm.java.net/oqlhelp.html
   - HAT [Heap Analysis Tool](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jhat.html)
     Run `jhat java_pid<PID>.hprof` and open http://localhost:7000/ with your web browser.
   - [Eclipse Memory Analyzer](http://eclipse.org/mat/)
     This is an Eclipse plugin which provides nice graphical browsing through the heap dump
     and has several useful reports like memory leaks, wasted memory etc.


Further options
---------------
CPAchecker exports several values via the JMX interface.
These can be watched during the runtime of CPAchecker.

1. Start VisualVM (`jvisualvm` from Ubuntu package `visualvm`).
2. In "Tools" -> "Plugins", install "VisualVM-MBeans" plugin.
3. Run CPAchecker.
4. In VisualVM, connect to CPAchecker process, switch to "MBeans" tab,
   and browse to the values you are interested in.
5. By double-clicking on a numerical value you can open a chart with this value per time.

Documentation: http://visualvm.java.net/mbeans_tab.html
