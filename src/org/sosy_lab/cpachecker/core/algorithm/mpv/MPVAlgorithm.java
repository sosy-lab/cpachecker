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
package org.sosy_lab.cpachecker.core.algorithm.mpv;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpv.partition.Partition;
import org.sosy_lab.cpachecker.core.algorithm.mpv.partition.PartitioningOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpv.partition.SeparatePartitioningOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.AbstractSingleProperty;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties.PropertySeparator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

@Options(prefix = "mpv")
public class MPVAlgorithm implements Algorithm, StatisticsProvider {

  public static class MPVStatistics implements Statistics {

    private final Timer totalTimer = new Timer();
    private final Timer createPartitionsTimer = new Timer();

    private int iterationNumber = 0;
    private final List<Partition> partitions = Lists.newArrayList();
    private MultipleProperties multipleProperties;

    private Collection<Statistics> statistics;

    public Result adjustOverallResult() {
      return multipleProperties.getOverallResult();
    }

    public void printResults(PrintStream out) {
      out.println("Result per each property:");
      for (AbstractSingleProperty property : multipleProperties.getProperties()) {
        out.println("  Property '" + property + "': " + property.getResult());
      }
    }

    @Override
    public String getName() {
      return "MPV algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      TimeSpan totalCpuTime = TimeSpan.ofNanos(0);
      for (AbstractSingleProperty property : multipleProperties.getProperties()) {
        totalCpuTime = TimeSpan.sum(totalCpuTime, property.getCpuTime());
      }
      out.println("Number of iterations:                         " + iterationNumber);
      out.println(
          "Total wall time for creating partitions:  "
              + createPartitionsTimer.getSumTime().formatAs(TimeUnit.SECONDS));
      out.println(
          "Total wall time for MPV algorithm:        "
              + totalTimer.getSumTime().formatAs(TimeUnit.SECONDS));
      out.println(
          "Total CPU time for MPV algorithm:         " + totalCpuTime.formatAs(TimeUnit.SECONDS));
      out.println();
      out.println("Partitions statisctics:");
      int counter = 1;
      for (Partition partition : partitions) {
        out.println("Partition " + counter++ + ":");
        out.println("  Properties (" + partition.getNumberOfProperties() + "): " + partition);
        out.println("  CPU time limit:\t" + partition.getTimeLimit().formatAs(TimeUnit.SECONDS));
        out.println("  Spent CPU time:\t" + partition.getSpentCPUTime().formatAs(TimeUnit.SECONDS));
      }
      out.println();
      out.println("Properties statistics:");
      for (AbstractSingleProperty property : multipleProperties.getProperties()) {
        Result result = property.getResult();
        out.println("Property '" + property + "'");
        out.println("  CPU time:" + property.getCpuTime().formatAs(TimeUnit.SECONDS));
        out.println("  Relevant:    " + property.isRelevant());
        out.println("  Result:      " + result);
        if (result.equals(Result.FALSE)) {
          out.println("    Found violations:     " + property.getViolations());
          out.println("    All violations found: " + property.isAllViolationsFound());
          out.println("    Description:          " + property.getViolatedPropertyDescription());
        }
        if ((result.equals(Result.FALSE) && !property.isAllViolationsFound())
            || result.equals(Result.UNKNOWN)) {
          out.println("    Reason of UNKNOWN:    " + property.getReasonOfUnknown());
        }
      }
    }
  }

  @Option(
      secure = true,
      name = "limits.cpuPerProperty",
      description =
          "Limit for cpu time per each property in MPV (use seconds or specify a unit; -1 for infinite)")
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
  private TimeSpan cpuPerProperty = TimeSpan.ofNanos(-1);

  @Option(secure = true, name = "propertySeparator", description = "...")
  private PropertySeparator propertySeparator = PropertySeparator.FILE;

  @Option(secure = true, name = "partitionOperator", description = "Partitioning operator for MPV.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.mpv.partition")
  @Nonnull
  private Class<? extends PartitioningOperator> partitioningOperatorClass =
      SeparatePartitioningOperator.class;

  @Option(
      secure = true,
      name = "findAllViolations",
      description = "Find all violations of each checked property.")
  private boolean findAllViolations = false;

  @Option(
      secure = true,
      name = "collectAllStatistics",
      description = "Collect statistics for all inner algorithm and for each iteration.")
  private boolean collectAllStatistics = false;

  private final MPVStatistics stats = new MPVStatistics();
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;
  private final CFA cfa;

  public MPVAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    cpa = pCpa;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    cfa = pCfa;
    config.inject(this);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    stats.totalTimer.start();

    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(reached.getFirstState());
    CFANode mainFunction = Iterables.getOnlyElement(initialNodes);
    stats.multipleProperties =
        new MultipleProperties(
            specification.getSpecification(), propertySeparator, findAllViolations);
    PartitioningOperator partitioningOperator = createPartitioningOperator();

    try {
      do { // for each new list of partitions
        for (Partition partition : partitioningOperator.createPartition()) {
          int numberOfProperties = partition.getNumberOfProperties();
          if (numberOfProperties <= 0) {
            // shortcut
            continue;
          }
          stats.partitions.add(partition);
          ShutdownManager shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
          ResourceLimitChecker limits =
              ResourceLimitChecker.setInnerLimit(logger, shutdownManager, partition.getTimeLimit());
          limits.start();

          stats.multipleProperties.setTargetProperties(partition.getProperties());

          // inner algorithm
          Algorithm algorithm = createInnerAlgorithm(reached, mainFunction, shutdownManager);
          collectStatistics(algorithm);
          try {
            partition.startAnalysis();
            logger.log(
                Level.INFO,
                "Iteration "
                    + stats.iterationNumber
                    + ": checking partition "
                    + partition
                    + " with "
                    + numberOfProperties
                    + " properties");
            do {
              status = status.update(algorithm.run(reached));
            } while (!partition.isChecked(reached));
            logger.log(Level.INFO, "Stopping iteration " + stats.iterationNumber);
          } catch (InterruptedException e) {
            if (shutdownNotifier.shouldShutdown()) {
              // Interrupted by outer limit checker or by user
              logger.logUserException(Level.WARNING, e, "Analysis interrupted from the outside");
              partition.stopAnalysisOnFailure(reached, "Interrupted");
              throw e;
            } else {
              // Interrupted by inner limit checker
              logger.log(Level.INFO, e, "Partition has exhausted resource limitations");
              partition.stopAnalysisOnFailure(reached, "Inner time limit");
            }
          } catch (Exception e) {
            // Try to intercept any exception, which may be related to checking of specific
            // property, so it would be possible to successfully check other properties.
            logger.log(Level.WARNING, e, ": Exception during partition checking");
            partition.stopAnalysisOnFailure(reached, e.getClass().getSimpleName());
          } finally {
            limits.cancel();
          }
        }
      } while (!stats.multipleProperties.isChecked());
    } finally {
      stats.totalTimer.stop();
    }
    return status;
  }

  private void collectStatistics(Algorithm pAlgorithm) {
    if (collectAllStatistics) {
      if (pAlgorithm instanceof StatisticsProvider) {
        ((StatisticsProvider) pAlgorithm).collectStatistics(stats.statistics);
      }
    }
  }

  private PartitioningOperator createPartitioningOperator() throws CPAException {
    try {
      Constructor<?> partitioningOperatorConstructor =
          partitioningOperatorClass.getConstructor(
              Configuration.class, MultipleProperties.class, TimeSpan.class);
      return (PartitioningOperator)
          partitioningOperatorConstructor.newInstance(
              config, stats.multipleProperties, cpuPerProperty);
    } catch (NoSuchMethodException
        | SecurityException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      logger.log(
          Level.SEVERE,
          "Cannot instantiate partitioning operator " + partitioningOperatorClass + ": " + e);
      throw new CPAException("Cannot instantiate partitioning operator: " + e);
    }
  }

  private Algorithm createInnerAlgorithm(
      ReachedSet reached, CFANode mainFunction, ShutdownManager shutdownManager)
      throws InterruptedException, CPAException {
    try {
      stats.createPartitionsTimer.start();
      if (stats.iterationNumber > 0) {
        // Clear reached set for further iterations.
        reached.clear();
        AbstractState initialState =
            cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
        Precision initialPrecision =
            cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());
        reached.add(initialState, initialPrecision);
      }
      stats.iterationNumber++;

      ConfigurationBuilder innerConfigBuilder = Configuration.builder();
      innerConfigBuilder.copyFrom(config);
      innerConfigBuilder.clearOption("analysis.algorithm.MPV"); // to prevent infinite recursion
      Configuration singleConfig = innerConfigBuilder.build();
      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              singleConfig, logger, shutdownManager.getNotifier(), new AggregatedReachedSets());

      return coreComponents.createAlgorithm(cpa, cfa, specification);
    } catch (InvalidConfigurationException e) {
      // should be unreachable, since configuration is already checked
      throw new CPAException("Cannot create configuration for inner algorithm: " + e);
    } finally {
      stats.createPartitionsTimer.stop();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    stats.statistics = pStatsCollection;
  }
}
