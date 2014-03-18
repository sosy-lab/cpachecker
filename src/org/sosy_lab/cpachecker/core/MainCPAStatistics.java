/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.management.JMException;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.CoverageInformation;
import org.sosy_lab.cpachecker.util.resources.MemoryStatistics;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

@Options
class MainCPAStatistics implements Statistics {

  // Beyond this many states, we omit some statistics because they are costly.
  private static final int MAX_SIZE_FOR_REACHED_STATISTICS = 1000000;

  @Option(name="reachedSet.export",
      description="print reached set to text file")
  private boolean exportReachedSet = true;

  @Option(name="reachedSet.file",
      description="print reached set to text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFile = Paths.get("reached.txt");

  @Option(name="coverage.export",
      description="print coverage info to file")
  private boolean exportCoverage = true;

  @Option(name="coverage.file",
      description="print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Paths.get("coverage.%s.info");

  @Option(name="statistics.memory",
    description="track memory usage of JVM during runtime")
  private boolean monitorMemoryUsage = true;

  private final LogManager logger;
  private final Collection<Statistics> subStats;
  private final MemoryStatistics memStats;
  private Thread memStatsThread;

  private final Timer programTime = new Timer();
  final Timer creationTime = new Timer();
  final Timer cpaCreationTime = new Timer();
  private final Timer analysisTime = new Timer();

  private long programCpuTime;
  private long analysisCpuTime = 0;

  private Statistics cfaCreatorStatistics;
  private CFA cfa;

  public MainCPAStatistics(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);

    subStats = new ArrayList<>();

    if (monitorMemoryUsage) {
      memStats = new MemoryStatistics(pLogger);
      memStatsThread = Threads.newThread(memStats, "CPAchecker memory statistics collector", true);
      memStatsThread.start();
    } else {
      memStats = null;
    }

    programTime.start();
    try {
      programCpuTime = ProcessCpuTime.read();
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the cpu time, some statistics will be missing.");
      programCpuTime = -1;
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAChecker from crashing in this case we catch the error and log the event.
     */
    catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
      programCpuTime = -1;
    }
  }

  public Collection<Statistics> getSubStatistics() {
    return subStats;
  }

  @Override
  public String getName() {
    return "CPAchecker";
  }

  void startAnalysisTimer() {
    analysisTime.start();
    try {
      analysisCpuTime = ProcessCpuTime.read();
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      // user was already warned
      analysisCpuTime = -1;
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAChecker from crashing in this case we catch the error and log the event.
     */
    catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
      analysisCpuTime = -1;
    }
  }

  void stopAnalysisTimer() {
    analysisTime.stop();
    programTime.stop();

    try {
      long stopCpuTime = ProcessCpuTime.read();

      if (programCpuTime >= 0) {
        programCpuTime = stopCpuTime - programCpuTime;
      }
      if (analysisCpuTime >= 0) {
        analysisCpuTime = stopCpuTime - analysisCpuTime;
      }

    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      // user was already warned
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAChecker from crashing in this case we catch the error and log the event.
     */
    catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    checkNotNull(out);
    checkNotNull(result);
    checkArgument(result == Result.NOT_YET_STARTED || reached != null);

    // call stop again in case CPAchecker was terminated abnormally
    if (analysisTime.isRunning()) {
      analysisTime.stop();
    }
    if (programTime.isRunning()) {
      programTime.stop();
    }
    if (memStats != null) {
      memStatsThread.interrupt(); // stop memory statistics collection
    }

    if (result != Result.NOT_YET_STARTED) {
      dumpReachedSet(reached);

      printSubStatistics(out, result, reached);

      if (exportCoverage && outputCoverageFile != null && cfa != null) {
        CoverageInformation.writeCoverageInfo(outputCoverageFile, reached, cfa, logger);
      }
    }

    out.println("CPAchecker general statistics");
    out.println("-----------------------------");

    printCfaStatistics(out);

    if (result != Result.NOT_YET_STARTED) {
      try {
        printReachedSetStatistics(reached, out);
      } catch (OutOfMemoryError e) {
        logger.logUserException(Level.WARNING, e,
            "Out of memory while generating statistics about final reached set");
      }
    }

    out.println();

    printTimeStatistics(out, result, reached);

    out.println();

    printMemoryStatistics(out);
  }

  private void dumpReachedSet(ReachedSet reached) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    if (exportReachedSet && outputFile != null) {
      try (Writer w = Files.openOutputFile(outputFile)) {
        Joiner.on('\n').appendTo(w, reached);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write reached set to file");
      } catch (OutOfMemoryError e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write reached set to file due to memory problems");
      }
    }
  }

  private void printSubStatistics(PrintStream out, Result result, ReachedSet reached) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

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
  }

  private void printReachedSetStatistics(ReachedSet reached, PrintStream out) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    if (reached instanceof ForwardingReachedSet) {
      reached = ((ForwardingReachedSet)reached).getDelegate();
    }
    int reachedSize = reached.size();

    out.println("Size of reached set:             " + reachedSize);

    if (!reached.isEmpty()) {
      if (reachedSize < MAX_SIZE_FOR_REACHED_STATISTICS) {
        printReachedSetStatisticsDetails(reached, out);
      }

      if (reached.hasWaitingState()) {
        out.println("  Size of final wait list        " + reached.getWaitlistSize());
      }
    }
  }

  private void printReachedSetStatisticsDetails(ReachedSet reached, PrintStream out) {
    int reachedSize = reached.size();
    Set<CFANode> locations;
    CFANode mostFrequentLocation = null;
    int mostFrequentLocationCount = 0;

    if (reached instanceof LocationMappedReachedSet) {
      LocationMappedReachedSet l = (LocationMappedReachedSet)reached;
      locations = l.getLocations();

      Map.Entry<Object, Collection<AbstractState>> maxPartition = l.getMaxPartition();
      mostFrequentLocation = (CFANode)maxPartition.getKey();
      mostFrequentLocationCount = maxPartition.getValue().size();

    } else {
      HashMultiset<CFANode> allLocations = HashMultiset.create(from(reached)
                                                                    .transform(EXTRACT_LOCATION)
                                                                    .filter(notNull()));

      locations = allLocations.elementSet();

      for (Multiset.Entry<CFANode> location : allLocations.entrySet()) {
        int size = location.getCount();
        if (size > mostFrequentLocationCount) {
          mostFrequentLocationCount = size;
          mostFrequentLocation = location.getElement();
        }
      }
    }

    if (!locations.isEmpty()) {
      int locs = locations.size();
      out.println("  Number of reached locations:   " + locs + " (" + StatisticsUtils.toPercent(locs, cfa.getAllNodes().size()) + ")");
      out.println("    Avg states per location:     " + reachedSize / locs);
      out.println("    Max states per location:     " + mostFrequentLocationCount + " (at node " + mostFrequentLocation + ")");

      Set<String> functions = from(locations).transform(CFAUtils.GET_FUNCTION).toSet();
      out.println("  Number of reached functions:   " + functions.size() + " (" + StatisticsUtils.toPercent(functions.size(), cfa.getNumberOfFunctions()) + ")");
    }

    if (reached instanceof PartitionedReachedSet) {
      PartitionedReachedSet p = (PartitionedReachedSet)reached;
      int partitions = p.getNumberOfPartitions();
      out.println("  Number of partitions:          " + partitions);
      out.println("    Avg size of partitions:      " + reachedSize / partitions);
      Map.Entry<Object, Collection<AbstractState>> maxPartition = p.getMaxPartition();
      out.print  ("    Max size of partitions:      " + maxPartition.getValue().size());
      if (maxPartition.getValue().size() > 1) {
        out.println(" (with key " + maxPartition.getKey() + ")");
      } else {
        out.println();
      }
    }
    out.println("  Number of target states:       " + from(reached).filter(IS_TARGET_STATE).size());
  }

  private void printCfaStatistics(PrintStream out) {
    if (cfa != null) {

      out.println("Number of program locations:     " + cfa.getAllNodes().size());
      out.println("Number of functions:             " + cfa.getNumberOfFunctions());

      if (cfa.getLoopStructure().isPresent()) {
        int loops = cfa.getLoopStructure().get().values().size();
        out.println("Number of loops:                 " + loops);
      }
    }
  }

  private void printTimeStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("Time for analysis setup:      " + creationTime);
    out.println("  Time for loading CPAs:      " + cpaCreationTime);
    if (cfaCreatorStatistics != null) {
      cfaCreatorStatistics.printStatistics(out, result, reached);
    }
    out.println("Time for Analysis:            " + analysisTime);
    out.println("CPU time for analysis:        " + TimeSpan.ofNanos(analysisCpuTime).formatAs(TimeUnit.SECONDS));
    out.println("Total time for CPAchecker:    " + programTime);
    out.println("Total CPU time for CPAchecker:" + TimeSpan.ofNanos(programCpuTime).formatAs(TimeUnit.SECONDS));
  }

  private void printMemoryStatistics(PrintStream out) {
    if (monitorMemoryUsage) {
      MemoryStatistics.printGcStatistics(out);

      if (memStats != null) {
        try {
          memStatsThread.join(); // thread should have terminated already,
                                 // but wait for it to ensure memory visibility
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        if (!memStatsThread.isAlive()) {
          memStats.printStatistics(out);
        }
      }
    }
  }

  public void setCFACreator(CFACreator pCfaCreator) {
    Preconditions.checkState(cfaCreatorStatistics == null);
    cfaCreatorStatistics = pCfaCreator.getStatistics();
  }

  public void setCFA(CFA pCfa) {
    Preconditions.checkState(cfa == null);
    cfa = pCfa;
  }
}
