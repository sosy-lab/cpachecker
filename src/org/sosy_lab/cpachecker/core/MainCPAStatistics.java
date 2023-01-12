// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.management.JMException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.AbstractBAMCPA;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.coverage.CoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportGcov;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportStdoutSummary;
import org.sosy_lab.cpachecker.util.cwriter.CExpressionInvariantExporter;
import org.sosy_lab.cpachecker.util.resources.MemoryStatistics;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.SolverException;

@Options
class MainCPAStatistics implements Statistics {

  // Beyond this many states, we omit some statistics because they are costly.
  private static final int MAX_SIZE_FOR_REACHED_STATISTICS = 1000000;

  @Option(secure = true, name = "reachedSet.export", description = "print reached set to text file")
  private boolean exportReachedSet = false;

  @Option(secure = true, name = "reachedSet.file", description = "print reached set to text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reachedSetFile = Path.of("reached.txt");

  @Option(secure = true, name = "reachedSet.dot", description = "print reached set to graph file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reachedSetGraphDumpPath = Path.of("reached.dot");

  @Option(
      secure = true,
      name = "statistics.memory",
      description = "track memory usage of JVM during runtime")
  private boolean monitorMemoryUsage = true;

  @Option(
      secure = true,
      name = "cinvariants.export",
      description = "Output an input file, with invariants embedded as assume constraints.")
  private boolean cInvariantsExport = false;

  @Option(
      secure = true,
      name = "cinvariants.prefix",
      description = "Prefix to add to an output file, which would contain assumed invariants.")
  @FileOption(Type.OUTPUT_FILE)
  private @Nullable PathTemplate cInvariantsPrefix = PathTemplate.ofFormatString("inv-%s");

  @Option(
      secure = true,
      name = "coverage.enabled",
      description = "Compute and export information about the verification coverage?")
  private boolean exportCoverage = true;

  @Option(secure = true, name = "coverage.file", description = "print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Path.of("coverage.info");

  private final LogManager logger;
  private final Collection<Statistics> subStats;
  private final @Nullable MemoryStatistics memStats;
  private final @Nullable CExpressionInvariantExporter cExpressionInvariantExporter;
  private Thread memStatsThread;

  private final Timer programTime = new Timer();
  final Timer creationTime = new Timer();
  final Timer cpaCreationTime = new Timer();
  private final Timer analysisTime = new Timer();
  final Timer resultAnalysisTime = new Timer();

  private long programCpuTime;
  private long analysisCpuTime = 0;

  private @Nullable Statistics cfaCreatorStatistics;
  private @Nullable CFA cfa;
  private @Nullable ConfigurableProgramAnalysis cpa;

  public MainCPAStatistics(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    logger = pLogger;
    pConfig.inject(this);

    subStats = new ArrayList<>();

    if (monitorMemoryUsage) {
      memStats = new MemoryStatistics(pLogger);
      memStatsThread =
          Concurrency.newDaemonThread("CPAchecker memory statistics collector", memStats);
      memStatsThread.start();
    } else {
      memStats = null;
    }

    programTime.start();
    try {
      programCpuTime = ProcessCpuTime.read();
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(
          Level.WARNING,
          "Your Java VM does not support measuring the cpu time, some statistics will be missing.");
      programCpuTime = -1;
    }
    /*
     * Google App Engine does not allow to use classes from the package java.lang.management.
     * Therefore it throws a NoClassDefFoundError if this is attempted regardless. To prevent
     * CPAchecker from crashing in this case we catch the error and log the event.
     */
    catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
      programCpuTime = -1;
    }

    if (cInvariantsExport && cInvariantsPrefix != null) {
      cExpressionInvariantExporter =
          new CExpressionInvariantExporter(pConfig, pLogger, pShutdownNotifier, cInvariantsPrefix);
    } else {
      cExpressionInvariantExporter = null;
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
     * CPAchecker from crashing in this case we catch the error and log the event.
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
     * CPAchecker from crashing in this case we catch the error and log the event.
     */
    catch (NoClassDefFoundError e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Google App Engine does not support measuring the cpu time.");
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
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

    final Timer statisticsTime = new Timer();
    statisticsTime.start();

    if (result != Result.NOT_YET_STARTED) {
      dumpReachedSet(reached);
      printSubStatistics(out, result, reached);
      exportCoverage(out, reached);
    }

    out.println();
    out.println("CPAchecker general statistics");
    out.println("-----------------------------");

    printCfaStatistics(out);

    if (result != Result.NOT_YET_STARTED) {
      try {
        printReachedSetStatistics(reached, out);
      } catch (OutOfMemoryError e) {
        logger.logUserException(
            Level.WARNING, e, "Out of memory while generating statistics about final reached set");
      }

      if (cExpressionInvariantExporter != null && cfa != null) {
        try {
          cExpressionInvariantExporter.exportInvariant(cfa, reached);
        } catch (IOException e) {
          logger.logUserException(
              Level.WARNING,
              e,
              "Encountered IO error while generating the invariant as an output program.");
        } catch (InterruptedException pE) {
          logger.logUserException(
              Level.WARNING, pE, "Interrupted while generating the invariant as an output program");
        } catch (SolverException e) {
          logger.logUserException(
              Level.WARNING,
              e,
              "Encountered solver problem while generating the invariant as an output program");
        }
      }
    }

    out.println();

    printTimeStatistics(out, result, reached, statisticsTime);

    out.println();

    printMemoryStatistics(out);
  }

  private void exportCoverage(PrintStream out, UnmodifiableReachedSet reached) {
    if (exportCoverage && cfa != null && reached.size() > 1) {
      FluentIterable<AbstractState> reachedStates = FluentIterable.from(reached);

      // hack to get all reached states for BAM
      if (cpa instanceof AbstractBAMCPA) {
        Collection<ReachedSet> otherReachedSets =
            ((AbstractBAMCPA) cpa).getData().getCache().getAllCachedReachedStates();
        reachedStates = reachedStates.append(FluentIterable.concat(otherReachedSets));
      }

      CoverageData infosPerFile = CoverageCollector.fromReachedSet(reachedStates, cfa);

      out.println();
      out.println("Code Coverage");
      out.println("-----------------------------");
      CoverageReportStdoutSummary.write(infosPerFile, out);

      if (outputCoverageFile != null) {
        try (Writer gcovOut = IO.openOutputFile(outputCoverageFile, Charset.defaultCharset())) {
          CoverageReportGcov.write(infosPerFile, gcovOut);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write coverage information to file");
        }
      }
    }
  }

  private void dumpReachedSet(UnmodifiableReachedSet reached) {
    dumpReachedSet(reached, reachedSetFile, false);
    dumpReachedSet(reached, reachedSetGraphDumpPath, true);
  }

  private void dumpReachedSet(
      UnmodifiableReachedSet reached, Path pOutputFile, boolean writeDotFormat) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    if (exportReachedSet && pOutputFile != null) {
      try (Writer w = IO.openOutputFile(pOutputFile, Charset.defaultCharset())) {

        if (writeDotFormat) {

          // Location-map specific dump.
          dumpLocationMappedReachedSet(reached, w);
        } else {

          // Default dump.
          Joiner.on('\n').appendTo(w, reached);
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write reached set to file");
      } catch (OutOfMemoryError e) {
        logger.logUserException(
            Level.WARNING, e, "Could not write reached set to file due to memory problems");
      }
    }
  }

  private void dumpLocationMappedReachedSet(final UnmodifiableReachedSet pReachedSet, Appendable sb)
      throws IOException {
    final ListMultimap<CFANode, AbstractState> locationIndex =
        Multimaps.index(pReachedSet, AbstractStates::extractLocation);

    DOTBuilder.generateDOT(sb, cfa, node -> formatCFANodeWithStateInformation(node, locationIndex));
  }

  private static String formatCFANodeWithStateInformation(
      CFANode node, Multimap<CFANode, AbstractState> locationMapping) {
    StringBuilder buf = new StringBuilder();
    buf.append(node.getNodeNumber()).append("\n");
    for (AbstractState state : locationMapping.get(node)) {
      if (state instanceof Graphable) {
        buf.append(((Graphable) state).toDOTLabel());
      }
    }
    return buf.toString();
  }

  private void printSubStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    for (Statistics s : subStats) {
      StatisticsUtils.printStatistics(s, out, logger, result, reached);
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    assert pReached != null : "ReachedSet may be null only if analysis not yet started";

    for (Statistics s : subStats) {
      StatisticsUtils.writeOutputFiles(s, logger, pResult, pReached);
    }
  }

  private void printReachedSetStatistics(UnmodifiableReachedSet reached, PrintStream out) {
    assert reached != null : "ReachedSet may be null only if analysis not yet started";

    if (reached instanceof ForwardingReachedSet) {
      reached = ((ForwardingReachedSet) reached).getDelegate();
    }
    int reachedSize = reached.size();

    out.println("Size of reached set:             " + reachedSize);

    if (!reached.isEmpty()) {
      if (reachedSize < MAX_SIZE_FOR_REACHED_STATISTICS) {
        printReachedSetStatisticsDetails(reached, out);
      }

      if (reached.hasWaitingState()) {
        out.println("  Size of final wait list:       " + reached.getWaitlist().size());
      }
    }
  }

  private void printReachedSetStatisticsDetails(UnmodifiableReachedSet reached, PrintStream out) {
    int reachedSize = reached.size();
    Set<CFANode> locations;
    CFANode mostFrequentLocation = null;
    int mostFrequentLocationCount = 0;

    if (reached instanceof LocationMappedReachedSet) {
      LocationMappedReachedSet l = (LocationMappedReachedSet) reached;
      locations = l.getLocations();

      Map.Entry<Object, Collection<AbstractState>> maxPartition = l.getMaxPartition();
      mostFrequentLocation = (CFANode) maxPartition.getKey();
      mostFrequentLocationCount = maxPartition.getValue().size();

    } else {
      Multiset<CFANode> allLocations =
          from(reached).transform(AbstractStates::extractLocation).filter(notNull()).toMultiset();
      locations = allLocations.elementSet();

      for (Multiset.Entry<CFANode> location : allLocations.entrySet()) {
        int size = location.getCount();
        if (size > mostFrequentLocationCount) {
          mostFrequentLocationCount = size;
          mostFrequentLocation = location.getElement();

        } else if (size == mostFrequentLocationCount) {
          // use node with smallest number to have deterministic output
          mostFrequentLocation =
              Ordering.natural().min(mostFrequentLocation, location.getElement());
        }
      }
    }

    if (!locations.isEmpty()) {
      int locs = locations.size();
      out.println(
          "  Number of reached locations:   "
              + locs
              + " ("
              + StatisticsUtils.toPercent(locs, cfa.getAllNodes().size())
              + ")");
      out.println("    Avg states per location:     " + reachedSize / locs);
      out.println(
          "    Max states per location:     "
              + mostFrequentLocationCount
              + " (at node "
              + mostFrequentLocation
              + ")");

      long functions = locations.stream().map(CFANode::getFunctionName).distinct().count();
      out.println(
          "  Number of reached functions:   "
              + functions
              + " ("
              + StatisticsUtils.toPercent(functions, cfa.getNumberOfFunctions())
              + ")");
    }

    if (reached instanceof PartitionedReachedSet) {
      PartitionedReachedSet p = (PartitionedReachedSet) reached;
      int partitions = p.getNumberOfPartitions();
      out.println("  Number of partitions:          " + partitions);
      out.println("    Avg size of partitions:      " + reachedSize / partitions);
      Map.Entry<Object, Collection<AbstractState>> maxPartition = p.getMaxPartition();
      out.print("    Max size of partitions:      " + maxPartition.getValue().size());
      if (maxPartition.getValue().size() > 1) {
        out.println(" (with key " + maxPartition.getKey() + ")");
      } else {
        out.println();
      }
    }
    out.println(
        "  Number of target states:       "
            + from(reached).filter(AbstractStates::isTargetState).size());
  }

  private void printCfaStatistics(PrintStream out) {
    if (cfa != null) {
      StatisticsWriter.writingStatisticsTo(out)
          .put("Number of program locations", cfa.getAllNodes().size())
          .put(
              StatInt.forStream(
                  StatKind.SUM,
                  "Number of CFA edges (per node)",
                  cfa.getAllNodes().stream().mapToInt(CFANode::getNumLeavingEdges)))
          .putIfPresent(
              cfa.getVarClassification(),
              "Number of relevant variables",
              vc -> vc.getRelevantVariables().size())
          .put("Number of functions", cfa.getNumberOfFunctions())
          .putIfPresent(
              cfa.getLoopStructure(),
              loops ->
                  StatInt.forStream(
                      StatKind.COUNT,
                      "Number of loops (and loop nodes)",
                      loops.getAllLoops().stream().mapToInt(loop -> loop.getLoopNodes().size())));
    }
  }

  private void printTimeStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached, Timer statisticsTime) {
    out.println("Time for analysis setup:      " + creationTime);
    out.println("  Time for loading CPAs:      " + cpaCreationTime);
    if (cfaCreatorStatistics != null) {
      StatisticsUtils.printStatistics(cfaCreatorStatistics, out, logger, result, reached);
      StatisticsUtils.writeOutputFiles(cfaCreatorStatistics, logger, result, reached);
    }
    out.println("Time for Analysis:            " + analysisTime);
    out.println(
        "CPU time for analysis:        "
            + TimeSpan.ofNanos(analysisCpuTime).formatAs(TimeUnit.SECONDS));
    if (resultAnalysisTime.getNumberOfIntervals() > 0) {
      out.println("Time for analyzing result:    " + resultAnalysisTime);
    }
    out.println("Total time for CPAchecker:    " + programTime);
    out.println(
        "Total CPU time for CPAchecker:"
            + TimeSpan.ofNanos(programCpuTime).formatAs(TimeUnit.SECONDS));
    out.println("Time for statistics:          " + statisticsTime);
  }

  private void printMemoryStatistics(PrintStream out) {
    MemoryStatistics.printGcStatistics(out);

    if (monitorMemoryUsage && memStats != null) {
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

  public void setCFACreator(CFACreator pCfaCreator) {
    Preconditions.checkState(cfaCreatorStatistics == null);
    cfaCreatorStatistics = pCfaCreator.getStatistics();
  }

  public void setCFA(CFA pCfa) {
    Preconditions.checkState(cfa == null);
    cfa = pCfa;
  }

  public void setCPA(ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkState(cpa == null);
    cpa = pCpa;
  }
}
