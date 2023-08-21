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
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA;
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
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class implements a backward bounded model checking (BBMC) algorithm as described in section
 * 2 and 3 of the paper "Backward Symbolic Execution with Loop Folding" by Marek Chalupa and Jan
 * Strejček.
 *
 * <p>The algorithm unrolls the CFA backwards, starting from the error locations, up to a loop
 * bound. It then considers paths from an error location to the entry of the program. If the path
 * formula of a target (entry) state is SAT, the property is false, i.e. an error location is
 * reachable. Otherwise, the algorithm tests the path formulas corresponding to the loop heads for
 * satisfiability. If both the entry of the program and the loop heads are unreachable, the property
 * is proved. Else, the loop bound is incremented and the CFA is unrolled further.
 */
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
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  private BMCStatistics stats;

  protected final ShutdownNotifier shutdownNotifier;
  private final TargetLocationProvider targetLocationProvider;

  public BackwardBMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      final ShutdownManager pShutdownManager,
      CFA pCFA)
      throws InvalidConfigurationException {

    pConfig.inject(this, BackwardBMCAlgorithm.class);

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

    shutdownNotifier = pShutdownManager.getNotifier();
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    // These are the states with error labels
    Set<AbstractState> errorEntryStates = reachedSet.asCollection();
    if (errorEntryStates.isEmpty()) {
      logger.log(Level.INFO, "No starting error locations found...");
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    AlgorithmStatus status;
    do {
      status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);

      if (FluentIterable.from(reachedSet)
          // Entry states are abstraction states
          .filter(state -> !errorEntryStates.contains(state))
          // target states (main entry) may also be abstraction states
          .filter(Predicates.not(AbstractStates::isTargetState))
          .anyMatch(PredicateAbstractState::containsAbstractionState)) {
        logger.log(
            Level.WARNING,
            "Backward BMC algorithm does not work with abstractions. Could not check for"
                + " satisfiability!");
        return status;
      }
      shutdownNotifier.shutdownIfNecessary();

      FluentIterable<AbstractState> targetStates = getTargetStates(reachedSet);
      if (targetStates.isEmpty()) {
        logger.log(Level.INFO, "No path to target found...");
        continue;
      }
      // Only consider the loop head states that were most recently unrolled
      // as the algorithm only wants to test whether the loop heads
      // of the most recent unrolling are satisfiable
      FluentIterable<AbstractState> latestLoopHeadStates = getRecentLoopHeadStates(reachedSet);

      final boolean targetReachable = isAnyReachable(targetStates);
      final boolean loopHeadsReachable = isAnyReachable(latestLoopHeadStates);
      if (targetReachable) {
        InterpolationHelper.removeUnreachableTargetStates(reachedSet);
        // if all targets and loop heads are unreachable, the result is sound
        if (loopHeadsReachable) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      } else {
        // target is reachable: found error path
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }

    } while (status.isSound() && adjustConditions());

    // Could not find a satisfiable path to target within unrolling bound
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private FluentIterable<AbstractState> getTargetStates(final ReachedSet reachedSet) {
    return FluentIterable.from(reachedSet).filter(AbstractStates::isTargetState);
  }

  private boolean isAnyReachable(FluentIterable<AbstractState> pStates)
      throws CPAException, InterruptedException {
    // Returns disjunction of path formulas
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
      safe = solver.isUnsat(formula);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      stats.satCheck.stop();
    }

    return safe;
  }

  // Returns the abstract states corresponding to loop head locations
  // of the most recent unrolling of the program loops
  private FluentIterable<AbstractState> getRecentLoopHeadStates(final ReachedSet pReachedSet) {
    Set<CFANode> loopHeadNodes = BMCHelper.getLoopHeads(cfa, targetLocationProvider);
    FluentIterable<AbstractState> allLoopHeadStates =
        AbstractStates.filterLocations(pReachedSet, loopHeadNodes);
    final int currentIteration = CPAs.retrieveCPA(cpa, LoopBoundCPA.class).getMaxLoopIterations();
    return BMCHelper.filterIteration(allLoopHeadStates, currentIteration, loopHeadNodes);
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
