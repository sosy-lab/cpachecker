// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.SelectionAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class EquivalenceRunner {

  public record SafeAndUnsafeConstraints(
      AlgorithmStatus status,
      ImmutableList<BooleanFormula> safe,
      ImmutableList<BooleanFormula> unsafe,
      ImmutableList<Integer> touchedLines) {}

  private record AnalysisComponents(
      Algorithm algorithm, ConfigurableProgramAnalysis cpa, CFA cfa, ReachedSet reached) {}

  private final Specification specification;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;

  public EquivalenceRunner(
      Specification pSpecification,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdown) {
    specification = pSpecification;
    config = pConfig;
    logger = pLogger;
    shutdown = pShutdown;
  }

  private ReachedSet createInitialReachedSet(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      CoreComponentsFactory pFactory,
      LogManager singleLogger)
      throws InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pFactory.createReachedSet(cpa);
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private AnalysisComponents createAnalysis(CFA pProgram)
      throws InvalidConfigurationException, InterruptedException, CPAException {
    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(config, logger, shutdown, AggregatedReachedSets.empty());
    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(pProgram, specification);
    Algorithm algorithm = coreComponents.createAlgorithm(cpa, pProgram, specification);
    return new AnalysisComponents(
        algorithm,
        cpa,
        pProgram,
        createInitialReachedSet(cpa, pProgram.getMainFunction(), coreComponents, logger));
  }

  private AlgorithmStatus runFullAnalysis(Algorithm pAlgorithm, ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    AlgorithmStatus status = pAlgorithm.run(reachedSet);
    if (pAlgorithm instanceof SelectionAlgorithm s) {
      pAlgorithm = s.getLastAlgorithm();
    }
    if (pAlgorithm instanceof RestartAlgorithm r) {
      pAlgorithm = r.getCurrentAlgorithm();
    }
    while (reachedSet.hasWaitingState()) {
      for (AbstractState abstractState : reachedSet) {
        if (((ARGState) abstractState).isTarget()) {
          reachedSet.removeOnlyFromWaitlist(abstractState);
        }
      }
      status = status.update(pAlgorithm.run(reachedSet));
    }
    return status;
  }

  public SafeAndUnsafeConstraints runStrategy(CFA program, LeafStrategy strategy)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    AnalysisComponents analysis = createAnalysis(program);
    AlgorithmStatus status = runFullAnalysis(analysis.algorithm(), analysis.reached());
    return strategy.export(analysis.reached(), analysis.cfa(), status);
  }
}
