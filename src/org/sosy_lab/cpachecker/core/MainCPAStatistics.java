/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core;

import static org.sosy_lab.cpachecker.util.AbstractStates.filterTargetStates;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;

@Options
class MainCPAStatistics implements Statistics {

  /**
   * Some hints on memory usage numbers that I have found out so far
   * (as of 2011-11-21 with OpenJDK 6 on Linux, obtained by pwendler).
   *
   * There are four ways to get memory numbers:
   * 1) The relevant methods in the {@link Runtime} class.
   *    (Java heap memory)
   * 2) The {@link java.lang.management.MemoryMXBean}.
   *    (Java heap and non-heap memory)
   * 3) The {@link java.lang.management.MemoryPoolMXBean}.
   *    (Java heap and non-heap memory per memory pool)
   * 4) The method {@link com.sun.management.OperatingSystemMXBean#getCommittedVirtualMemorySize()}.
   *    (Total memory usage of process)
   *
   * 1) gives the same numbers as 2) for the heap memory.
   * The sum of heap and non-heap given by 2) is the same as the sum of all pools from 3).
   *
   * 3) has the benefit of also providing peak usage and "collection usage" numbers,
   * although I currently don't know how helpful they are. "Collection usage" is
   * defined as the memory that was used after the JVM "most recently expended
   * effort in recycling unused objects in this memory pool"
   * (i.e., performed garbage collection). These numbers are not available for all
   * memory pools. There is also support for defining usage thresholds which will
   * result in MBean notifications being emitted.
   *
   * 4) is only supported on Sun-family JVMs (at least the method is defined only
   * in internal JVM classes). I do not know whether this method works on other OS.
   * On Linux this gives the same number as the "top" command in the "VIRT" column.
   * According to the man page this includes "all code, data and shared libraries
   * plus pages that pages that have been mapped but not used" (and thus this
   * measure includes more than we would like).
   *
   *
   * With the {@link java.lang.management.MemoryPoolMXBean}, one can configure
   * thresholds for notification when they are full. There is also a threshold
   * for notification when they are full even after a GC
   * (see {@link java.lang.management.MemoryPoolMXBean#setCollectionUsageThreshold(long)}).
   * However, as of 2012-04-02 with OpenJDK 6 on Linux, this is not really
   * helpful. First, there is one pool (the "Survivor" pool), which supports
   * thresholds but has a weird maximum size set (the pool grows beyond the
   * maximum size). Second, there seem to be a lot of notifications even while
   * GC is still running. I still haven't found a way how to reliably detect
   * that an OutOfMemoryError would come soon.
   */
  private static class MemoryStatistics extends Thread {

    private static final long MEMORY_CHECK_INTERVAL = 100; // milliseconds

    private final LogManager logger;

    private long maxHeap = 0;
    private long sumHeap = 0;
    private long maxHeapAllocated = 0;
    private long sumHeapAllocated = 0;
    private long maxNonHeap = 0;
    private long sumNonHeap = 0;
    private long maxNonHeapAllocated = 0;
    private long sumNonHeapAllocated = 0;
    private long maxProcess = 0;
    private long sumProcess = 0;
    private long count = 0;

    // necessary stuff to query the OperatingSystemMBean
    private final MBeanServer mbeanServer;
    private ObjectName osMbean;
    private static final String MEMORY_SIZE = "CommittedVirtualMemorySize";

    private MemoryStatistics(LogManager pLogger) {
      super("CPAchecker memory statistics collector");

      logger = pLogger;

      mbeanServer = ManagementFactory.getPlatformMBeanServer();
      try {
        osMbean = new ObjectName("java.lang", "type", "OperatingSystem");
      } catch (MalformedObjectNameException e) {
        logger.logDebugException(e, "Accessing OperatingSystemMXBean failed");
        osMbean = null;
      }
    }

    @Override
    public void run() {
      MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();

      while (true) { // no stop condition, call Thread#interrupt() to stop it
        count++;

        // get Java heap usage
        MemoryUsage currentHeap = mxBean.getHeapMemoryUsage();
        long currentHeapUsed = currentHeap.getUsed();
        maxHeap = Math.max(maxHeap, currentHeapUsed);
        sumHeap += currentHeapUsed;

        long currentHeapAllocated = currentHeap.getCommitted();
        maxHeapAllocated = Math.max(maxHeapAllocated, currentHeapAllocated);
        sumHeapAllocated += currentHeapAllocated;

        // get Java non-heap usage
        MemoryUsage currentNonHeap = mxBean.getNonHeapMemoryUsage();
        long currentNonHeapUsed = currentNonHeap.getUsed();
        maxNonHeap = Math.max(maxNonHeap, currentNonHeapUsed);
        sumNonHeap += currentNonHeapUsed;

        long currentNonHeapAllocated = currentNonHeap.getCommitted();
        maxNonHeapAllocated = Math.max(maxNonHeapAllocated, currentNonHeapAllocated);
        sumNonHeapAllocated += currentNonHeapAllocated;

        // get process virtual memory usage
        if (osMbean != null) {
          try {
            long memUsed = (Long) mbeanServer.getAttribute(osMbean, MEMORY_SIZE);
            maxProcess = Math.max(maxProcess, memUsed);
            sumProcess += memUsed;

          } catch (JMException e) {
            logger.logDebugException(e, "Querying memory size failed");
            osMbean = null;
          } catch (ClassCastException e) {
            logger.logDebugException(e, "Querying memory size failed");
            osMbean = null;
          }
        }

        try {
          sleep(MEMORY_CHECK_INTERVAL);
        } catch (InterruptedException e) {
          return; // force thread exit
        }
      }
    }

  }

    @Option(name="reachedSet.export",
        description="print reached set to text file")
    private boolean exportReachedSet = true;

    @Option(name="reachedSet.file",
        description="print reached set to text file")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private File outputFile = new File("reached.txt");

    @Option(name="statistics.memory",
      description="track memory usage of JVM during runtime")
    private boolean monitorMemoryUsage = true;

    private final LogManager logger;
    private final Collection<Statistics> subStats;
    private final MemoryStatistics memStats;

    final Timer programTime = new Timer();
    final Timer creationTime = new Timer();
    final Timer cpaCreationTime = new Timer();
    final Timer analysisTime = new Timer();

    private CFACreator cfaCreator;

    public MainCPAStatistics(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
        logger = pLogger;
        config.inject(this);

        subStats = new ArrayList<Statistics>();

        if (monitorMemoryUsage) {
          memStats = new MemoryStatistics(pLogger);
          memStats.setDaemon(true);
          memStats.start();
        } else {
          memStats = null;
        }

        programTime.start();
    }

    public Collection<Statistics> getSubStatistics() {
      return subStats;
  }

    @Override
    public String getName() {
        return "CPAchecker";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        // call stop again in case CPAchecker was terminated abnormally
        if (analysisTime.isRunning()) {
          analysisTime.stop();
        }
        if (programTime.isRunning()) {
          programTime.stop();
        }
        if (memStats != null) {
          memStats.interrupt(); // stop memory statistics collection
        }

        if (exportReachedSet && outputFile != null) {
          try {
            Files.writeFile(outputFile, Joiner.on('\n').join(reached));
          } catch (IOException e) {
            logger.logUserException(Level.WARNING, e, "Could not write reached set to file");
          } catch (OutOfMemoryError e) {
            logger.logUserException(Level.WARNING, e,
                "Could not write reached set to file due to memory problems");
          }
        }

        for (Statistics s : subStats) {
          String name = s.getName();
          if (!Strings.isNullOrEmpty(name)) {
            name = name + " statistics";
            out.println(name);
            out.println(Strings.repeat("-", name.length()));
          }

          try {
            s.printStatistics(out, result, reached);
          } catch (OutOfMemoryError e) {
            logger.logUserException(Level.WARNING, e,
                "Out of memory while generating statistics and writing output files");
          }

          if (!Strings.isNullOrEmpty(name)) {
            out.println();
          }
        }
        // In theory, we could catch OOM in the following code, too.
        // However, usually the statistics are not the problematic part,
        // only the output files. Thus we don't bother here.

        if (reached instanceof ForwardingReachedSet) {
          reached = ((ForwardingReachedSet)reached).getDelegate();
        }
        int reachedSize = reached.size();

        out.println("CPAchecker general statistics");
        out.println("-----------------------------");
        out.println("Size of reached set:          " + reachedSize);

        if (reached instanceof LocationMappedReachedSet) {
          LocationMappedReachedSet l = (LocationMappedReachedSet)reached;
          int locs = l.getNumberOfPartitions();
          if (locs > 0) {
            out.println("  Number of locations:        " + locs);
            out.println("    Avg states per loc.:      " + reachedSize / locs);
            Map.Entry<Object, Collection<AbstractState>> maxPartition = l.getMaxPartition();
            out.println("    Max states per loc.:      " + maxPartition.getValue().size() + " (at node " + maxPartition.getKey() + ")");
          }

        } else {
          HashMultiset<CFANode> allLocations = HashMultiset.create(AbstractStates.extractLocations(reached));
          int locs = allLocations.entrySet().size();
          if (locs > 0) {
            out.println("  Number of locations:        " + locs);
            out.println("    Avg states per loc.:      " + reachedSize / locs);

            int max = 0;
            CFANode maxLoc = null;
            for (Multiset.Entry<CFANode> location : allLocations.entrySet()) {
              int size = location.getCount();
              if (size > max) {
                max = size;
                maxLoc = location.getElement();
              }
            }
            out.println("    Max states per loc.:      " + max + " (at node " + maxLoc + ")");
          }
        }

        if (reached instanceof PartitionedReachedSet) {
          PartitionedReachedSet p = (PartitionedReachedSet)reached;
          int partitions = p.getNumberOfPartitions();
          out.println("  Number of partitions:       " + partitions);
          out.println("    Avg size of partitions:   " + reachedSize / partitions);
          Map.Entry<Object, Collection<AbstractState>> maxPartition = p.getMaxPartition();
          out.print  ("    Max size of partitions:   " + maxPartition.getValue().size());
          if (maxPartition.getValue().size() > 1) {
            out.println(" (with key " + maxPartition.getKey() + ")");
          } else {
            out.println();
          }
        }
        out.println("  Number of target elements:  " + Iterables.size(filterTargetStates(reached)));
        out.println("Time for analysis setup:      " + creationTime);
        out.println("  Time for loading CPAs:      " + cpaCreationTime);
        if (cfaCreator != null) {
          out.println("  Time for loading C parser:  " + cfaCreator.parserInstantiationTime);
          out.println("  Time for CFA construction:  " + cfaCreator.totalTime);
          out.println("    Time for parsing C file:  " + cfaCreator.parsingTime);
          out.println("    Time for AST to CFA:      " + cfaCreator.conversionTime);
          out.println("    Time for CFA sanity check:" + cfaCreator.checkTime);
          out.println("    Time for post-processing: " + cfaCreator.processingTime);
          if (cfaCreator.pruningTime.getNumberOfIntervals() > 0) {
            out.println("    Time for CFA pruning:     " + cfaCreator.pruningTime);
          }
          if (cfaCreator.exportTime.getNumberOfIntervals() > 0) {
            out.println("    Time for CFA export:      " + cfaCreator.exportTime);
          }
        }
        out.println("Time for Analysis:            " + analysisTime);
        out.println("Total time for CPAchecker:    " + programTime);

        out.println("");
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Set<String> gcNames = new HashSet<String>();
        long gcTime = 0;
        int gcCount = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
          gcTime += gcBean.getCollectionTime();
          gcCount += gcBean.getCollectionCount();
          gcNames.add(gcBean.getName());
        }
        out.println("Time for Garbage Collector:   " + Timer.formatTime(gcTime) + " (in " + gcCount + " runs)");
        out.println("Garbage Collector(s) used:    " + Joiner.on(", ").join(gcNames));

        if (memStats != null) {
          try {
            memStats.join(); // thread should have terminated already,
                             // but wait for it to ensure memory visibility
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          out.println("Used heap memory:             " + formatMem(memStats.maxHeap) + " max (" + formatMem(memStats.sumHeap/memStats.count) + " avg)");
          out.println("Used non-heap memory:         " + formatMem(memStats.maxNonHeap) + " max (" + formatMem(memStats.sumNonHeap/memStats.count) + " avg)");
          out.println("Allocated heap memory:        " + formatMem(memStats.maxHeapAllocated) + " max (" + formatMem(memStats.sumHeapAllocated/memStats.count) + " avg)");
          out.println("Allocated non-heap memory:    " + formatMem(memStats.maxNonHeapAllocated) + " max (" + formatMem(memStats.sumNonHeapAllocated/memStats.count) + " avg)");
          if (memStats.osMbean != null) {
            out.println("Total process virtual memory: " + formatMem(memStats.maxProcess) + " max (" + formatMem(memStats.sumProcess/memStats.count) + " avg)");
          }
        }
    }

    private static String formatMem(long mem) {
      return String.format("%,9dMB", mem >> 20);
    }

    public void setCFACreator(CFACreator pCfaCreator) {
      Preconditions.checkState(cfaCreator == null);
      cfaCreator = pCfaCreator;
    }
}
