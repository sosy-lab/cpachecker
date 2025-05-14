// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.detailed_counterexample_export.DetailedCounterexampleExport;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.TraversalMethod;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "sampling.random")
public class RandomSamplingAlgorithm implements Algorithm {

  @Option(
      secure = true,
      description = "File name for analysis report in case a counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate counterexampleExport =
      PathTemplate.ofFormatString("Counterexample.trace.%d.txt");

  @Option(
      secure = true,
      description = "File name for analysis report in case no counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate safeExport = PathTemplate.ofFormatString("Safe.trace.%d.txt");

  @Option(
      secure = true,
      description =
          "Amount of samples to be generated. Generate an infinite amount of samples with a value"
              + " less than 0.")
  private int samplesToBeGenerated = 10;

  private int exportedCounterexampleCount = 0;

  private int exportedSafeTracesCount = 0;

  private Algorithm algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier notifier;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis cpa;
  private final AssumptionToEdgeAllocator allocator;
  private final DetailedCounterexampleExport exporter;

  public RandomSamplingAlgorithm(
      Algorithm pAlgorithm,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pNotifier,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    logger = pLogger;
    notifier = pNotifier;
    cfa = pCfa;
    cpa = pCpa;
    algorithm = pAlgorithm;
    allocator = AssumptionToEdgeAllocator.create(config, logger, cfa.getMachineModel());
    // TODO: Refactor this such that the export can be used statically
    exporter = new DetailedCounterexampleExport(pAlgorithm, pConfig, pLogger, pNotifier, pCfa);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // Copy the current reached set

    checkArgument(
        (reachedSet.getFirstState() instanceof ARGState),
        "The reached set must contain an ARGState as the first state.");

    ARGState firstStateOriginalArg = (ARGState) reachedSet.getFirstState();
    ARGState clonedArgState =
        new ARGState(
            firstStateOriginalArg.getWrappedState(), null /* The first state has no parent */);
    // We explicitly do not do CEGAR in the value analysis for sampling, so we do not need
    // to copy the precision.
    Precision precision = reachedSet.getPrecision(firstStateOriginalArg);

    boolean counterexampleFound = false;

    while (exportedSafeTracesCount + exportedCounterexampleCount < samplesToBeGenerated
        || samplesToBeGenerated < 0) {
      notifier.shutdownIfNecessary();

      ARGState newInitialState =
          new ARGState(clonedArgState.getWrappedState(), null /* The first state has no parent */);

      ReachedSet newReachedSet = new PartitionedReachedSet(cpa, TraversalMethod.DFS);
      newReachedSet.add(newInitialState, precision);

      // run the algorithm
      algorithm.run(newReachedSet);

      // export the resulting trace
      exportReachedSet(newReachedSet);

      boolean isSafeTrace =
          FluentIterable.from(newReachedSet).filter(AbstractStates::isTargetState).isEmpty();
      counterexampleFound = counterexampleFound || !isSafeTrace;
    }

    if (counterexampleFound) {
      // Add a dummy target state
      reachedSet.add(
          new ARGState(firstStateOriginalArg.getWrappedState(), firstStateOriginalArg) {

            @Serial private static final long serialVersionUID = 2608790182643565040L;

            @Override
            public boolean isTarget() {
              return true;
            }

            @Override
            public Set<TargetInformation> getTargetInformation() throws IllegalStateException {
              return ImmutableSet.of();
            }
          },
          precision);
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  void exportReachedSet(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    List<ARGState> sortedById =
        FluentIterable.from(pReachedSet.asCollection())
            .filter(ARGState.class)
            .toSortedList(Comparator.naturalOrder());
    ARGState lastOne = sortedById.get(sortedById.size() - 1);

    Set<ARGPath> reachablePaths = ARGUtils.getAllPaths(pReachedSet, lastOne);
    Verify.verify(reachablePaths.size() == 1);
    ARGPath singlePath = FluentIterable.from(reachablePaths).get(0);

    CFAPathWithAssumptions pathWithAssumptions =
        CFAPathWithAssumptions.of(
            Objects.requireNonNull(singlePath), pReachedSet.getCPA(), allocator);
    CounterexampleInfo trace = CounterexampleInfo.feasiblePrecise(singlePath, pathWithAssumptions);

    boolean isSafeTrace =
        FluentIterable.from(pReachedSet).filter(AbstractStates::isTargetState).isEmpty();
    Path exportPath;
    if (isSafeTrace) {
      exportPath = safeExport.getPath(exportedSafeTracesCount++);
    } else {
      exportPath = counterexampleExport.getPath(exportedCounterexampleCount++);
    }

    try {
      exporter.exportErrorInducingInputs(trace, exportPath);
    } catch (IOException | InvalidConfigurationException | SolverException e) {
      logger.logUserException(Level.WARNING, e, "Could not export trace inputs.");
    }
  }
}
