// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeErrorException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;

/**
 * This class is a runnable that continuously monitors memory usage. To use it, instantiate it, and
 * let a {@link Thread} run it. Call {@link Thread#interrupt()} when you want to stop monitoring,
 * wait for termination with {@link Thread#join()}. Then check if the thread is alive with {@link
 * Thread#isAlive()} and call {@link #printStatistics(PrintStream)} if the thread is NOT alive.
 *
 * <p>It also provides a static utility method for printing garbage collection statistics.
 *
 * <p>Some hints on memory usage numbers that I have found out so far (as of 2011-11-21 with OpenJDK
 * 6 on Linux, obtained by pwendler).
 *
 * <p>There are four ways to get memory numbers: 1) The relevant methods in the {@link Runtime}
 * class. (Java heap memory) 2) The {@link java.lang.management.MemoryMXBean}. (Java heap and
 * non-heap memory) 3) The {@link java.lang.management.MemoryPoolMXBean}. (Java heap and non-heap
 * memory per memory pool) 4) The method
 * com.sun.management.OperatingSystemMXBean#getCommittedVirtualMemorySize(). (Total memory usage of
 * process)
 *
 * <p>1) gives the same numbers as 2) for the heap memory. The sum of heap and non-heap given by 2)
 * is the same as the sum of all pools from 3).
 *
 * <p>3) has the benefit of also providing peak usage and "collection usage" numbers, although I
 * currently don't know how helpful they are. "Collection usage" is defined as the memory that was
 * used after the JVM "most recently expended effort in recycling unused objects in this memory
 * pool" (i.e., performed garbage collection). These numbers are not available for all memory pools.
 * There is also support for defining usage thresholds which will result in MBean notifications
 * being emitted.
 *
 * <p>4) is only supported on Sun-family JVMs (at least the method is defined only in internal JVM
 * classes). I do not know whether this method works on other OS. On Linux this gives the same
 * number as the "top" command in the "VIRT" column. According to the man page this includes "all
 * code, data and shared libraries plus pages that pages that have been mapped but not used" (and
 * thus this measure includes more than we would like).
 *
 * <p>With the {@link java.lang.management.MemoryPoolMXBean}, one can configure thresholds for
 * notification when they are full. There is also a threshold for notification when they are full
 * even after a GC (see {@link
 * java.lang.management.MemoryPoolMXBean#setCollectionUsageThreshold(long)}). However, as of
 * 2012-04-02 with OpenJDK 6 on Linux, this is not really helpful. First, there is one pool (the
 * "Survivor" pool), which supports thresholds but has a weird maximum size set (the pool grows
 * beyond the maximum size). Second, there seem to be a lot of notifications even while GC is still
 * running. I still haven't found a way how to reliably detect that an OutOfMemoryError would come
 * soon.
 */
public class MemoryStatistics implements Runnable {

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

  private final MemoryPoolMXBean[] pools;
  private final long[] sumHeapAllocatedPerPool;
  private final long[] maxHeapAllocatedPerPool;

  private long maxProcess = 0;
  private long sumProcess = 0;

  private long count = 0;
  private long errorCount = 0;

  private final MemoryMXBean memory;

  // necessary stuff to query the OperatingSystemMBean
  private final MBeanServer mbeanServer;
  private ObjectName osMbean;
  private static final String MEMORY_SIZE = "CommittedVirtualMemorySize";

  /**
   * Instantiate this thread. You need to call {@link Thread#start()} afterwards to start measuring.
   */
  public MemoryStatistics(LogManager pLogger) {
    //    super("CPAchecker memory statistics collector");

    logger = pLogger;
    memory = ManagementFactory.getMemoryMXBean();

    mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      osMbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
    } catch (MalformedObjectNameException e) {
      logger.logDebugException(e, "Accessing OperatingSystemMXBean failed");
      osMbean = null;
    }

    List<MemoryPoolMXBean> poolList = new ArrayList<>(2);
    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
      String name = pool.getName();
      if (name.contains("Old")) {
        poolList.add(pool);
      }
    }

    pools = poolList.toArray(new MemoryPoolMXBean[0]);
    sumHeapAllocatedPerPool = new long[pools.length];
    maxHeapAllocatedPerPool = new long[pools.length];
  }

  @Override
  public void run() {
    while (true) { // no stop condition, call Thread#interrupt() to stop it
      count++;

      // get Java heap usage
      final MemoryUsage currentHeap;
      try {
        currentHeap = memory.getHeapMemoryUsage();
      } catch (IllegalArgumentException e) {
        // Java 11 produces this with msg "committed = 3146776576 should be < max = 3145728000":
        // https://bugs.openjdk.java.net/browse/JDK-8207200
        // It is just about statistics, so we do not care if it happens from time to time,
        // but we want to see if it happens often and makes our statistics unreliable.
        errorCount++;
        continue;
      }
      long currentHeapUsed = currentHeap.getUsed();
      maxHeap = Math.max(maxHeap, currentHeapUsed);
      sumHeap += currentHeapUsed;

      long currentHeapAllocated = currentHeap.getCommitted();
      maxHeapAllocated = Math.max(maxHeapAllocated, currentHeapAllocated);
      sumHeapAllocated += currentHeapAllocated;

      // get Java non-heap usage
      final MemoryUsage currentNonHeap;
      try {
        currentNonHeap = memory.getNonHeapMemoryUsage();
      } catch (IllegalArgumentException e) {
        errorCount++; // cf. above
        continue;
      }
      long currentNonHeapUsed = currentNonHeap.getUsed();
      maxNonHeap = Math.max(maxNonHeap, currentNonHeapUsed);
      sumNonHeap += currentNonHeapUsed;

      long currentNonHeapAllocated = currentNonHeap.getCommitted();
      maxNonHeapAllocated = Math.max(maxNonHeapAllocated, currentNonHeapAllocated);
      sumNonHeapAllocated += currentNonHeapAllocated;

      for (int i = 0; i < pools.length; i++) {
        long currentPoolUsage = pools[i].getUsage().getUsed();
        maxHeapAllocatedPerPool[i] = Math.max(maxHeapAllocatedPerPool[i], currentPoolUsage);
        sumHeapAllocatedPerPool[i] += currentPoolUsage;
      }

      // get process virtual memory usage
      if (osMbean != null) {
        try {
          long memUsed = (Long) mbeanServer.getAttribute(osMbean, MEMORY_SIZE);
          maxProcess = Math.max(maxProcess, memUsed);
          sumProcess += memUsed;

        } catch (RuntimeErrorException e) {
          throw ManagementUtils.handleRuntimeErrorException(e);
        } catch (JMException | ClassCastException e) {
          logger.logDebugException(e, "Querying memory size failed");
          osMbean = null;
        }
      }

      try {
        Thread.sleep(MEMORY_CHECK_INTERVAL);
      } catch (InterruptedException e) {
        return; // force thread exit
      }
    }
  }

  /**
   * Print the gathered statistics. This method may only be called when the thread running this
   * instance has finished! Check with {@link Thread#isAlive()} prior invocation!
   */
  public void printStatistics(PrintStream out) {
    long heapPeak = 0;
    long nonHeapPeak = 0;
    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
      long peak = pool.getPeakUsage().getUsed();
      if (pool.getType() == MemoryType.HEAP) {
        heapPeak += peak;
      } else {
        nonHeapPeak += peak;
      }
    }

    if (errorCount > 0) {
      out.println(
          String.format(
              "Memory-statistics error count: %d (memory statistics might be unreliable)",
              errorCount));
    }

    out.println(
        "Used heap memory:             "
            + formatMem(maxHeap)
            + " max; "
            + formatMem(sumHeap / count)
            + " avg; "
            + formatMem(heapPeak)
            + " peak");
    out.println(
        "Used non-heap memory:         "
            + formatMem(maxNonHeap)
            + " max; "
            + formatMem(sumNonHeap / count)
            + " avg; "
            + formatMem(nonHeapPeak)
            + " peak");

    for (int i = 0; i < pools.length; i++) {
      String name = Strings.padEnd("Used in " + pools[i].getName() + " pool:", 30, ' ');
      out.println(
          name
              + formatMem(maxHeapAllocatedPerPool[i])
              + " max; "
              + formatMem(sumHeapAllocatedPerPool[i] / count)
              + " avg; "
              + formatMem(pools[i].getPeakUsage().getUsed())
              + " peak");
    }

    out.println(
        "Allocated heap memory:        "
            + formatMem(maxHeapAllocated)
            + " max; "
            + formatMem(sumHeapAllocated / count)
            + " avg");
    out.println(
        "Allocated non-heap memory:    "
            + formatMem(maxNonHeapAllocated)
            + " max; "
            + formatMem(sumNonHeapAllocated / count)
            + " avg");

    if (osMbean != null) {
      out.println(
          "Total process virtual memory: "
              + formatMem(maxProcess)
              + " max; "
              + formatMem(sumProcess / count)
              + " avg");
    }
  }

  /**
   * Print some statistics about garbage collection. This method may always be called regardless of
   * whether the memory statistics thread was used.
   */
  public static void printGcStatistics(PrintStream out) {
    List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    Set<String> gcNames = new TreeSet<>();
    long gcTime = 0;
    long gcCount = 0;
    for (GarbageCollectorMXBean gcBean : gcBeans) {
      gcTime += gcBean.getCollectionTime();
      gcCount += gcBean.getCollectionCount();
      gcNames.add(gcBean.getName());
    }
    out.println(
        "Time for Garbage Collector:   "
            + TimeSpan.ofMillis(gcTime).formatAs(TimeUnit.SECONDS)
            + " (in "
            + gcCount
            + " runs)");
    out.println("Garbage Collector(s) used:    " + Joiner.on(", ").join(gcNames));
  }

  private static String formatMem(long mem) {
    return String.format("%6dMB (%6d MiB)", mem / 1000 / 1000, mem >> 20);
  }
}
