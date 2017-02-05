/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import com.google.common.collect.FluentIterable;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.ForwardTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A utility class with methods for instantiating formulas based on SSA indices of the provided
 * transition system.
 */
public final class PDRUtils {

  /** Cannot instantiate utility class. */
  private PDRUtils() {}

  /**
   * Instantiates pFormula so that it represents the variables after the transition encoded in the
   * transition system. These variables are the so-called "primed" ones.
   *
   * @param pFormula The formula to be instantiated.
   * @param pFmgr The formula manager used for instantiation.
   * @param pTrans The transition system providing the primed ssa context.
   * @return The instantiated formula representing the state after the transition.
   * @see #asUnprimed(BooleanFormula, FormulaManagerView, TransitionSystem)
   */
  public static BooleanFormula asPrimed(
      BooleanFormula pFormula, FormulaManagerView pFmgr, TransitionSystem pTrans) {
    return pFmgr.instantiate(pFormula, pTrans.getPrimedContext().getSsa());
  }

  /**
   * Instantiates pFormula so that it represents the variables before the transition encoded in the
   * transition system. These variables are the so-called "unprimed" ones.
   *
   * @param pFormula The formula to be instantiated.
   * @param pFmgr The formula manager used for instantiation.
   * @param pTrans The transition system providing the unprimed ssa context.
   * @return The instantiated formula representing the state before the transition.
   * @see #asPrimed(BooleanFormula, FormulaManagerView, TransitionSystem)
   */
  public static BooleanFormula asUnprimed(
      BooleanFormula pFormula, FormulaManagerView pFmgr, TransitionSystem pTrans) {
    return pFmgr.instantiate(pFormula, pTrans.getUnprimedContext().getSsa());
  }

  public static Optional<Block> getBlockToNextTargetLocation(
      StatesWithLocation pStates,
      TransitionSystem pTransition,
      ForwardTransition pForward,
      FormulaManagerView pFmgr,
      BooleanFormulaManager pBfmgr,
      Solver pSolver)
      throws CPAException, InterruptedException, SolverException {
    Set<CFANode> errorLocs = pTransition.getTargetLocations();
    FluentIterable<Block> oneStepReachableErrorLocations =
        pForward
            .getBlocksFrom(pStates.getLocation())
            .filter(b -> errorLocs.contains(b.getSuccessorLocation()));

    if (oneStepReachableErrorLocations.isEmpty()) {
      return Optional.empty();
    }

    // If there is only one 1-step reachable error location for pState,
    // just return that block.
    if (oneStepReachableErrorLocations.size() == 1) {
      return Optional.of(oneStepReachableErrorLocations.first().get());
    }

    // Find the one that is reachable for pStates.
    for (Block b : oneStepReachableErrorLocations) {

      // Re-instantiate to match unprimed ssa indices of block; pc variable is still
      // present, but doesn't hurt.
      BooleanFormula reinstantiated = pFmgr.uninstantiate(pStates.getConcrete());
      reinstantiated = pFmgr.instantiate(reinstantiated, b.getUnprimedContext().getSsa());
      BooleanFormula transitionForBlock = pBfmgr.and(reinstantiated, b.getFormula());
      if (!pSolver.isUnsat(transitionForBlock)) {
        return Optional.of(b);
      }
    }

    return Optional.empty();
  }
}
