// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class wraps three formulas used in interpolation in order to avoid long parameter lists.
 * These formulas are: prefixFormula (from root to the first LH), loopFormula (from the first LH to
 * the second LH), and suffixFormula (from the second LH to targets). Note that prefixFormula is a
 * {@link PathFormula} as we need its {@link SSAMap} to update the SSA indices of derived
 * interpolants. TODO: update description
 */
class PartitionedFormulas {
  private final BooleanFormulaManagerView bfmgr;
  private final LogManager logger;
  private final boolean assertAllTargets;

  private BooleanFormula prefixFormula;
  private SSAMap prefixSsaMap;
  private List<BooleanFormula> loopFormulas;
  private BooleanFormula targetAssertion;

  PartitionedFormulas(
      BooleanFormulaManagerView bfmgr, LogManager logger, boolean assertAllTargets) {
    this.bfmgr = bfmgr;
    this.logger = logger;
    this.assertAllTargets = assertAllTargets;

    // TODO: check if initializing boolean formulas to true/false is safe
    prefixFormula = bfmgr.makeFalse();
    prefixSsaMap = SSAMap.emptySSAMap();
    loopFormulas = new ArrayList<>();
    targetAssertion = bfmgr.makeFalse();
  }

  int getNumLoops() {
    return loopFormulas.size();
  }

  SSAMap getPrefixSsaMap() {
    return prefixSsaMap;
  }

  BooleanFormula getPrefixFormula() {
    return prefixFormula;
  }

  List<BooleanFormula> getLoopFormulas() {
    return ImmutableList.copyOf(loopFormulas);
  }

  BooleanFormula getAssertionFormula() {
    return targetAssertion;
  }

  /**
   * A helper method to collect formulas needed by ISMC and IMC algorithm. It assumes every target
   * state after the loop has the same abstraction-state path to root. TODO: update description
   *
   * @param reachedSet Abstract Reachability Graph
   */
  void update(final ReachedSet reachedSet) {
    logger.log(Level.FINE, "Collecting BMC-partitioning formulas");
    FluentIterable<AbstractState> targetStatesAfterLoop =
        InterpolationHelper.getTargetStatesAfterLoop(reachedSet);
    if (targetStatesAfterLoop.isEmpty()) {
      // TODO: check if returning is safe, or an error/exception should be thrown
      return;
    }

    List<ARGState> abstractionStates =
        InterpolationHelper.getAbstractionStatesToRoot(targetStatesAfterLoop.get(0)).toList();

    // This assertion should pass as it has already been guarded by maxLoopIterations > 1
    assert abstractionStates.size() > 3;

    // collect prefix formula
    PathFormula prefixPathFormula =
        InterpolationHelper.getPredicateAbstractionBlockFormula(abstractionStates.get(1));
    prefixFormula = prefixPathFormula.getFormula();
    prefixSsaMap = prefixPathFormula.getSsa();

    // collect loop formulas
    loopFormulas.clear();
    for (int i = 2; i < abstractionStates.size() - 1; ++i) {
      // TR(V_k, V_k+1)
      loopFormulas.add(
          InterpolationHelper.getPredicateAbstractionBlockFormula(abstractionStates.get(i))
              .getFormula());
    }

    // collect target assertion formula
    BooleanFormula currentAssertion =
        InterpolationHelper.createDisjunctionFromStates(bfmgr, targetStatesAfterLoop);
    if (assertAllTargets) {
      // assume prefix and previous loop formulas remain the same after each unrolling
      // TODO: check if the assumption is correct
      // TODO: check if removing unreachable target states has any impact on this
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
}
