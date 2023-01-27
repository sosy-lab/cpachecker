// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides a formula representation for {@link IMCAlgorithm}. It stores the following
 * formulas.
 *
 * <ul>
 *   <li>{@link PartitionedFormulas#prefixFormula} (I): the block formula from root to the first
 *       loop head.
 *   <li>{@link PartitionedFormulas#loopFormulas} (T1, T2, ..., Tn): the block formulas between each
 *       consecutive pair of loop heads.
 *   <li>{@link PartitionedFormulas#targetAssertion} (&not;P): the block formula from the last to
 *       head to the target state. If {@link PartitionedFormulas#assertAllTargets} is set to true,
 *       the assertion formulas at every loop iterations are recorded and their disjunction is used;
 *       otherwise, only the assertion at the last iteration is kept.
 * </ul>
 */
class PartitionedFormulas {
  private static final String UNINITIALIZED_MSG =
      "The partitioned formulas have not been initialized yet, #collectFormulasFromARG must be"
          + " called beforehand.";

  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final boolean assertAllTargets;

  private boolean isInitialized;
  private PathFormula prefixFormula;
  private ImmutableList<PathFormula> loopFormulas;
  private BooleanFormula targetAssertion;

  PartitionedFormulas(
      PathFormulaManager pfmgr,
      BooleanFormulaManagerView bfmgr,
      LogManager logger,
      boolean assertAllTargets) {
    this.pfmgr = pfmgr;
    this.bfmgr = bfmgr;
    this.logger = logger;
    this.assertAllTargets = assertAllTargets;

    isInitialized = false;
    prefixFormula = InterpolationHelper.makeFalsePathFormula(pfmgr, bfmgr);
    loopFormulas = ImmutableList.of();
    targetAssertion = bfmgr.makeFalse();
  }

  /** Return the number of stored loop formulas. */
  int getNumLoops() {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return loopFormulas.size();
  }

  /** Return the SSA map of the prefix path formula. */
  SSAMap getPrefixSsaMap() {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return prefixFormula.getSsa();
  }

  /** Return the prefix formula (I) that describes the initial state set. */
  BooleanFormula getPrefixFormula() {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return prefixFormula.getFormula();
  }

  /** Return the SSA map of the specified loop. */
  SSAMap getSsaMapOfLoop(int loopIdx) {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return loopFormulas.get(loopIdx).getSsa();
  }

  /** Return the collected loop formulas (T1, T2, ..., Tn). */
  ImmutableList<BooleanFormula> getLoopFormulas() {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return transformedImmutableListCopy(loopFormulas, PathFormula::getFormula);
  }

  /** Return the collected loop formulas (T1, T2, ..., Tn). */
  BooleanFormula getLoopFormula(int loopIndex) {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return loopFormulas.get(loopIndex).getFormula();
  }

  /** Return the target assertion formula (&not;P). */
  BooleanFormula getAssertionFormula() {
    checkState(isInitialized, UNINITIALIZED_MSG);
    return targetAssertion;
  }

  /**
   * A helper method to collect and store formulas from the abstract reachability graph. It assumes
   * every target state after the loop has the same abstraction-state path to root.
   *
   * <p>If {@link #assertAllTargets} is set to true, it also assumes that after each unrolling, the
   * assertions at the previous loops stay valid, that is, the prefix and previous loop formulas
   * also remain the same.
   *
   * @param reachedSet Abstract Reachability Graph
   */
  void collectFormulasFromARG(final ReachedSet reachedSet) {
    logger.log(Level.FINE, "Collecting BMC-partitioning formulas");
    isInitialized = true;
    FluentIterable<AbstractState> targetStatesAfterLoop =
        InterpolationHelper.getTargetStatesAfterLoop(reachedSet);
    if (targetStatesAfterLoop.isEmpty()) {
      // no target is reachable, which means the program is safe
      prefixFormula = InterpolationHelper.makeFalsePathFormula(pfmgr, bfmgr);
      loopFormulas = ImmutableList.of();
      targetAssertion = bfmgr.makeFalse();
      return;
    }

    List<ARGState> abstractionStates =
        InterpolationHelper.getAbstractionStatesToRoot(targetStatesAfterLoop.get(0)).toList();

    // This assertion should pass as it has already been guarded by maxLoopIterations > 1
    assert abstractionStates.size() > 3;

    // collect prefix formula
    prefixFormula =
        InterpolationHelper.getPredicateAbstractionBlockFormula(abstractionStates.get(1));

    // collect loop formulas: TR(V_k, V_k+1)
    loopFormulas =
        transformedImmutableListCopy(
            abstractionStates.subList(2, abstractionStates.size() - 1),
            absState -> InterpolationHelper.getPredicateAbstractionBlockFormula(absState));

    // collect target assertion formula
    BooleanFormula currentAssertion =
        InterpolationHelper.createDisjunctionFromStates(bfmgr, targetStatesAfterLoop);
    if (assertAllTargets) {
      // disjunction with assertions at previous loops
      targetAssertion = bfmgr.or(targetAssertion, currentAssertion);
    } else {
      targetAssertion = currentAssertion;
    }

    assert !loopFormulas.isEmpty();
    logCollectedFormulas();
  }

  private void logCollectedFormulas() {
    if (loopFormulas.isEmpty()) {
      logger.log(Level.ALL, "No formulas collected yet");
      return;
    }
    logger.log(Level.ALL, "Prefix:", prefixFormula);
    for (int i = 0; i < loopFormulas.size(); ++i) {
      logger.log(Level.ALL, "Loop ", i, ": ", loopFormulas.get(i));
    }
    logger.log(Level.ALL, "Target:", targetAssertion);
  }

  /**
   * Check whether the given formula <i>f</i> is inductive w.r.t the transition relation <i>T</i>,
   * i.e. whether <i>f &and; T &rArr; f'</i>.
   *
   * @param f an uninstantiated formula
   */
  boolean checkInductivenessOf(Solver solver, BooleanFormula f)
      throws SolverException, InterruptedException {
    return checkRelativeInductivenssOf(solver, f, bfmgr.makeTrue());
  }

  /**
   * Check whether the given formula <i>f</i> is inductive relative to formula <i>g</i> w.r.t the
   * transition relation <i>T</i>, i.e. whether <i>f &and; g &and; T &rArr; f'</i>.
   *
   * @param f an uninstantiated formula
   * @param g an uninstantiated formula
   */
  boolean checkRelativeInductivenssOf(Solver solver, BooleanFormula f, BooleanFormula g)
      throws SolverException, InterruptedException {
    checkState(isInitialized, UNINITIALIZED_MSG);
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormula currentImage =
        bfmgr.and(fmgr.instantiate(f, getPrefixSsaMap()), fmgr.instantiate(g, getPrefixSsaMap()));
    BooleanFormula nextImage = fmgr.instantiate(f, getSsaMapOfLoop(0));
    return solver.implies(bfmgr.and(currentImage, getLoopFormula(0)), nextImage);
  }
}
