<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Profiling
=========

In order to profile CPAchecker in a meaningful way,
it is recommended to disable internal (costly) assertions with `-disable-java-assertions`
or even additionally disable optional features
like for [benchmarking](Benchmark.md) with `-benchmark`.

There exist many tools that allow to monitor Java processes, e.g. VisualVM.
Most of these connect to the JVM via a special file in `/tmp/hsperfdata_*`.
CPAchecker when started with `scripts/cpa.sh`
disables this file by default for performance reasons
(cf. [http://www.evanjones.ca/jvm-mmap-pause.html](http://www.evanjones.ca/jvm-mmap-pause.html)),
so these tools won't see the CPAchecker process.
Just run CPAchecker with the environment variable
`JAVA_VM_ARGUMENTS=-XX:-PerfDisableSharedMem` set to enable this again.


Java Flight Recorder
--------------------

[Java Flight Recorder](https://access.redhat.com/documentation/en-us/openjdk/17/html-single/using_jdk_flight_recorder_with_openjdk/index)
profiles time and memory consumption of individual methods and objects.
To profile CPAchecker with Java Flight Recorder,
set environment variable

```
JAVA_VM_ARGUMENTS="-XX:StartFlightRecording=filename=recording.jfr,dumponexit=true"
```

This records profiling samples during the full CPAchecker run and stores the recording in file `recording.jfr`.
It saves all recorded data when the JVM exits.

Recordings can be opened and analyzed with [Java Mission Control](https://github.com/openjdk/jmc)
or directly in IntelliJ IDEA.

### Example run

```
> JAVA_VM_ARGUMENTS="-XX:StartFlightRecording=filename=recording.jfr,dumponexit=true" scripts/cpa.sh -predicateAnalysis doc/examples/example_bug.c
Running CPAchecker with default heap size (1200M). Specify a larger value with -heap if you have more RAM.
Running CPAchecker with default stack size (1024k). Specify a larger value with -stack if needed.
Running CPAchecker with the following extra VM options: -XX:StartFlightRecording=filename=recording.jfr,dumponexit=true
[0.494s][info][jfr,startup] Started recording 1. No limit specified, using maxsize=250MB as default.
[0.494s][info][jfr,startup] 
[0.494s][info][jfr,startup] Use jcmd 455011 JFR.dump name=1 to copy recording data to file.
Language C detected and set for analysis (CPAMain.detectFrontendLanguageIfNecessary, INFO)

Using the following resource limits: CPU-time limit of 900s (ResourceLimitChecker.fromConfiguration, INFO)

CPAchecker 2.2.1-svn-44103M / predicateAnalysis (OpenJDK 64-Bit Server VM 19.0.2) started (CPAchecker.run, INFO)
[.. more output ..]
```


Time profiling
--------------

CPAchecker has internal time statistics, which are dumped into the output file `Statistics.txt`.
With the command-line argument `-stats` or the configuration option `statistics.print=true`
they are printed to the console, too.

VisualVM can profile CPU usage, but only with sampling.
It can only connect to a running process, so profiling of startup is not possible.

1. Start VisualVM (`visualvm` from Ubuntu package `visualvm`).
2. Run CPAchecker.
3. In VisualVM, connect to CPAchecker process, switch to tab `Sampler`
   and press `CPU` button.
4. Settings (for example sampling frequency and method filter)
   can be configured after setting the `Settings` checkbox in the upper right corner.

More information:
 - [documentation](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/profiler.html)
 - tutorial part [1](https://web.archive.org/web/20190117031705/https://blogs.oracle.com/nbprofiler/profiling-with-visualvm,-part-1) and [2](https://web.archive.org/web/20210414222507/https://blogs.oracle.com/nbprofiler/profiling-with-visualvm,-part-2)


Memory profiling
----------------

For graphing memory consumption over time:

1. Start VisualVM (`visualvm` from Ubuntu package `visualvm`).
2. In "Tools" -> "Plugins", install "Visual GC" plugin.
3. Run CPAchecker.
4. In VisualVM, connect to CPAchecker process,
   and look at "Monitor" and "Visual GC" tabs.

For viewing heap statistics (object count and memory usage per type):

1. Run `jmap -histo <PID>` where `<PID>` is the process ID of the running
   java process. This prints statistics of a current snapshot of the heap.

More information:
- [documentation](https://docs.oracle.com/en/java/javase/17/troubleshoot/diagnostic-tools.html#GUID-2E915FE8-A8A6-47C5-BA1D-4CC85174E818)
- [man page](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jmap.html)

For viewing detailed heap content after an `OutOfMemoryError`:

1. Set the option `-XX:+HeapDumpOnOutOfMemoryError` for the Java VM
   as described above.
2. Run CPAchecker and wait for `OutOfMemoryError`. A file `java_pid<PID>.hprof`
   is produced in the CPAchecker directory.
3. Analyze the heap dump with one of the following tools:
   - VisualVM (Package visualvm)
     Run "visualvm" and open the produced file.
     More information:
     - [Browsing heap dumps](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/heapdump.html)
     - [Querying heap dumps with OQL](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/oqlhelp.html)
   - [Eclipse Memory Analyzer](https://eclipse.dev/mat/)
     This is an Eclipse plugin which provides nice graphical browsing through the heap dump
     and has several useful reports like memory leaks, wasted memory etc.


Further options
---------------
CPAchecker exports several values via the JMX interface.
These can be watched during the runtime of CPAchecker.

1. Start VisualVM (`visualvm` from Ubuntu package `visualvm`).
2. In "Tools" -> "Plugins", install "VisualVM-MBeans" plugin.
3. Run CPAchecker.
4. In VisualVM, connect to CPAchecker process, switch to "MBeans" tab,
   and browse to the values you are interested in.
5. By double-clicking on a numerical value you can open a chart with this value per time.

More information:
- [documentation](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/mbeans_tab.html)
