// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "backwardBMC")
public class BackwardBMCAlgorithm implements Algorithm {

  @Option(
      secure = true,
      description =
          "Try to simplify the structure of formulas for the sat check of Backward BMC. "
              + "The improvement depends on the underlying SMT solver.")
  private boolean simplifyBooleanFormula = false;

  private LogManager logger;
  private Algorithm algorithm;
  private ConfigurableProgramAnalysis cpa;
  private CFA cfa;

  private final FormulaManagerView fmgr;
  // private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private BMCStatistics stats;

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
    stats = new BMCStatistics();
    algorithm = pAlgorithm;
    cpa = pCPA;
    cfa = pCFA;

    @SuppressWarnings("resource")
    PredicateCPA predCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, BackwardBMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    // pmgr = predCpa.getPathFormulaManager();

    shutdownNotifier = pShutdownManager.getNotifier();
    // is this the right target location provider?
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    if (reachedSet.isEmpty()) {
      logger.log(Level.INFO, "No starting error locations found...");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      do {

        AlgorithmStatus status;
        status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);

        if (FluentIterable.from(reachedSet)
            .skip(1) // first state of reached is always an abstraction state, so skip it
            .filter(Predicates.not(AbstractStates::isTargetState)) // target states may be
            // abstraction states
            .anyMatch(PredicateAbstractState::containsAbstractionState)) {

          logger.log(
              Level.WARNING,
              "Backward BMC algorithm does not work with abstractions. Could not check for"
                  + " satisfiability!");
          return status;
        }
        shutdownNotifier.shutdownIfNecessary();

        FluentIterable<AbstractState> targetState = getTarget(reachedSet);
        if (targetState.isEmpty()) {
          // No target state found
          return status;
        }
        FluentIterable<AbstractState> loopHeads = getAbstractLoopHeads(reachedSet);

        final boolean targetSafe = isSafe(targetState, prover);
        final boolean loopHeadsSafe = isSafe(loopHeads, prover);

        // if all loop heads are unsatisfiable, the result is sound
        if (targetSafe && loopHeadsSafe) {
          // How do we get the right result?
          reachedSet.remove(targetState.get(0));
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
        // if we have a satisfiable path to target, target is reachable
        if (!targetSafe) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }

      } while (adjustConditions());
    }

    return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
  }

  /** May return null if no target state found */
  private FluentIterable<AbstractState> getTarget(final ReachedSet reachedSet) {
    // Should be only one target state: the main entry; do we check this?
    return FluentIterable.from(reachedSet).filter(AbstractStates::isTargetState);
  }

  // Returns disjunction of path formulas
  private boolean isSafe(FluentIterable<AbstractState> pStates, ProverEnvironment pProver)
      throws CPAException, InterruptedException {
    BooleanFormula formula =
        BMCHelper.createFormulaFor(pStates, bfmgr, Optional.ofNullable(shutdownNotifier));

    if (simplifyBooleanFormula) {
      BigInteger sizeBeforeSimplification = fmgr.countBooleanOperations(formula);
      formula = fmgr.simplifyBooleanFormula(formula);
      BigInteger sizeAfterSimplification = fmgr.countBooleanOperations(formula);
      logger.logf(
          Level.FINER,
          "Formula was simplified from %s to %s boolean operations.",
          sizeBeforeSimplification,
          sizeAfterSimplification);
    }

    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    final boolean safe;
    try {
      pProver.push(formula);
      safe = pProver.isUnsat();
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      stats.satCheck.stop();
    }

    pProver.pop();

    return safe;
  }

  private FluentIterable<AbstractState> getAbstractLoopHeads(final ReachedSet reachedSet) {
    return AbstractStates.filterLocations(
        reachedSet, BMCHelper.getLoopHeads(cfa, targetLocationProvider));
  }

  /**
   * Adjusts the conditions of those CPAs that support the adjustment of conditions.
   *
   * @return {@code true} if the conditions were adjusted, {@code false} if no further adjustment is
   *     possible.
   */
  protected boolean adjustConditions() {
    FluentIterable<AdjustableConditionCPA> conditionCPAs =
        CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    boolean adjusted = conditionCPAs.anyMatch(AdjustableConditionCPA::adjustPrecision);
    if (!adjusted) {
      // these cpas said "do not continue"
      logger.log(
          Level.INFO,
          "Terminating because none of the following CPAs' precision can be adjusted any further ",
          conditionCPAs
              .transform(conditionCpa -> conditionCpa.getClass().getSimpleName())
              .join(Joiner.on(", ")));
    }
    return adjusted;
  }
}
