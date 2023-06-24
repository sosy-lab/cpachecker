// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import java.util.Optional;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BackwardBMCAlgorithm implements Algorithm {

  private LogManager logger;
  private Algorithm algorithm;
  private ConfigurableProgramAnalysis cpa;
  private CFA cfa;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  protected final ShutdownNotifier shutdownNotifier;
  private final TargetLocationProvider targetLocationProvider;

  public BackwardBMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      LogManager pLogger,
      final ShutdownManager pShutdownManager,
      CFA pCFA)
      throws InvalidConfigurationException {

    logger = pLogger;
    algorithm = pAlgorithm;
    cpa = pCPA;
    cfa = pCFA;

    @SuppressWarnings("resource")
    PredicateCPA predCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, BackwardBMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();

    shutdownNotifier = pShutdownManager.getNotifier();
    // is this the right target location provider?
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus status;
    status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);

    AbstractState targetState = getTarget(reachedSet);
    if (targetState == null) {
      // No target state found
      return status;
    }
    FluentIterable<AbstractState> loopHeads = getAbstractLoopHeads(reachedSet);

    BooleanFormula targetFormula =
        BMCHelper.createFormulaFor(
            FluentIterable.of(targetState),
            bfmgr,
            Optional.ofNullable(shutdownNotifier));
    BooleanFormula loopHeadFormula =
        BMCHelper.createFormulaFor(loopHeads, bfmgr, Optional.ofNullable(shutdownNotifier));

    return status;
  }

  /**
   * May return null if no target state found
   */
  private AbstractState getTarget(final ReachedSet reachedSet) {
    // Should be only one target state: the main entry
    return FluentIterable.from(reachedSet).filter(AbstractStates::isTargetState).first().orNull();
  }

  private FluentIterable<AbstractState> getAbstractLoopHeads(final ReachedSet reachedSet) {
    return AbstractStates
        .filterLocations(reachedSet, BMCHelper.getLoopHeads(cfa, targetLocationProvider));
  }
}
