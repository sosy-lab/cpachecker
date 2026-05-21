// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "sampling.random")
public class RandomSamplingAlgorithm implements Algorithm {

  @Option(secure = true, description = "Whether the random sampling algorithm exports traces.")
  private boolean exportTraces = false;

  @Option(
      secure = true,
      description = "File name for analysis report in case a counterexample was found.")
  @FileOption(Type.OUTPUT_FILE)
  private PathCounterTemplate counterexampleExport =
      PathCounterTemplate.ofFormatString("Counterexample.trace.%d.txt");

  @Option(
      secure = true,
      description = "File name for analysis report in case no counterexample was found.")
  @FileOption(Type.OUTPUT_FILE)
  private PathCounterTemplate safeExport = PathCounterTemplate.ofFormatString("Safe.trace.%d.txt");

  @Option(
      secure = true,
      description =
          "Amount of samples to be generated. Generate an infinite amount of samples with a value"
              + " less than 0.")
  private int samplesToBeGenerated = 10;

  @Option(
      secure = true,
      description =
          "Stop after finding the first counterexample. This is useful when using this as an actual"
              + " analysis.")
  private boolean stopAfterFirstCounterexample = false;

  private final LogManager logger;
  private final ShutdownNotifier notifier;

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  // for exporting
  private final PathChecker pathChecker;

  public RandomSamplingAlgorithm(
      Algorithm pAlgorithm,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pNotifier,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    notifier = pNotifier;
    cpa = pCpa;
    algorithm = pAlgorithm;
    Solver solver = Solver.create(pConfig, logger, notifier);
    PathFormulaManagerImpl pmgr =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(), pConfig, logger, notifier, pCfa, AnalysisDirection.FORWARD);
    pathChecker = new PathChecker(pConfig, logger, notifier, pCfa.getMachineModel(), pmgr, solver);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // Copy the current reached set

    checkArgument(
        (reachedSet.getFirstState() instanceof ARGState),
        "The reached set must contain an ARGState as the first state.");

    CFANode initialLocation = AbstractStates.extractLocation(reachedSet.getFirstState());
    checkNotNull(initialLocation, "The initial location must not be null.");

    AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    // generate at max samplesToBeGenerated samples (or infinite if samplesToBeGenerated < 0)
    for (int i = 0; i < samplesToBeGenerated || samplesToBeGenerated < 0; i++) {
      notifier.shutdownIfNecessary();

      reachedSet.clear();
      reachedSet.add(
          cpa.getInitialState(initialLocation, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(initialLocation, StateSpacePartition.getDefaultPartition()));

      // run the algorithm
      status = algorithm.run(reachedSet);

      if (reachedSet.getLastState() == null) {
        logger.log(
            Level.WARNING,
            "The last state of the reached set is null after running the random sampling"
                + " algorithm. Skipping trace export.");
        continue;
      }

      // export the resulting trace
      boolean traceIsCex = AbstractStates.isTargetState(reachedSet.getLastState());
      exportReachedSet(reachedSet, traceIsCex);

      if (stopAfterFirstCounterexample && traceIsCex) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
    }

    return status.update(AlgorithmStatus.UNSOUND_AND_PRECISE);
  }

  private Optional<Path> getExportPath(boolean pIsCex) {
    if (!exportTraces) {
      return Optional.empty();
    }
    if (pIsCex) {
      if (counterexampleExport == null) {
        return Optional.empty();
      }
      return Optional.of(counterexampleExport.getFreshPath());
    } else {
      if (safeExport == null) {
        return Optional.empty();
      }
      return Optional.of(safeExport.getFreshPath());
    }
  }

  private void exportReachedSet(ReachedSet pReachedSet, boolean pIsCex)
      throws InterruptedException {
    Optional<Path> optionalPath = getExportPath(pIsCex);
    if (optionalPath.isEmpty()) {
      return;
    }

    Path exportPath = optionalPath.orElseThrow();
    ARGPath trace =
        Iterables.getOnlyElement(
            ARGUtils.getAllPaths(pReachedSet, (ARGState) pReachedSet.getLastState()));
    CounterexampleInfo traceInfo =
        pathChecker.handleFeasibleCounterexample(
            CounterexampleTraceInfo.feasible(ImmutableList.of(), trace), trace);

    if (!traceInfo.isPreciseCounterExample()) {
      logger.log(Level.INFO, "Skipping export of imprecise counterexample.");
      return;
    }

    try {
      IO.writeFile(exportPath, Charset.defaultCharset(), traceInfo);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not export trace inputs.");
    }
  }
}
