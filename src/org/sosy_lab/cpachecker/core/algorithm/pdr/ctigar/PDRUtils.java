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

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Blocks;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.ForwardTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A utility class with methods for instantiating formulas based on SSA indices of the provided
 * transition system and for finding blocks that satisfy certain properties.
 */
public final class PDRUtils {

  /** Cannot instantiate utility class. */
  private PDRUtils() {}

  /**
   * Instantiates the states described by pFormula so that they represent the variables after the
   * transition encoded in the transition system. These variables are the so-called "primed" ones.
   *
   * @param pFormula The formula to be instantiated.
   * @param pFmgr The formula manager used for instantiation.
   * @param pTrans The transition system providing the primed ssa context.
   * @return The instantiated formula representing the states after the transition.
   * @see #asUnprimed(BooleanFormula, FormulaManagerView, TransitionSystem)
   */
  public static BooleanFormula asPrimed(
      BooleanFormula pFormula, FormulaManagerView pFmgr, TransitionSystem pTrans) {
    Objects.requireNonNull(pFormula);
    Objects.requireNonNull(pFmgr);
    Objects.requireNonNull(pTrans);
    return pFmgr.instantiate(pFormula, pTrans.getPrimedContext().getSsa());
  }

  /**
   * Instantiates the states described by pFormula so that they represent the variables before the
   * transition encoded in the transition system. These variables are the so-called "unprimed" ones.
   *
   * @param pFormula The formula to be instantiated.
   * @param pFmgr The formula manager used for instantiation.
   * @param pTrans The transition system providing the unprimed ssa context.
   * @return The instantiated formula representing the states before the transition.
   * @see #asPrimed(BooleanFormula, FormulaManagerView, TransitionSystem)
   */
  public static BooleanFormula asUnprimed(
      BooleanFormula pFormula, FormulaManagerView pFmgr, TransitionSystem pTrans) {
    Objects.requireNonNull(pFormula);
    Objects.requireNonNull(pFmgr);
    Objects.requireNonNull(pTrans);
    return pFmgr.instantiate(pFormula, pTrans.getUnprimedContext().getSsa());
  }

  /**
   * Checks whether pFormula is instantiated as an unprimed formula based on the context of the
   * transition system.
   *
   * @param pFormula The formula to be checked.
   * @param pFmgr The formula manager used for instantiation.
   * @param pTrans The transition system providing the unprimed ssa context.
   * @return True if pFormula is an unprimed formula, false otherwise.
   * @see #isPrimed(BooleanFormula, FormulaManagerView, TransitionSystem)
   */
  public static boolean isUnprimed(
      BooleanFormula pFormula, FormulaManagerView pFmgr, TransitionSystem pTrans) {
    Objects.requireNonNull(pFormula);
    Objects.requireNonNull(pFmgr);
    Objects.requireNonNull(pTrans);
    BooleanFormula formulaAsUnprimed =
        pFmgr.instantiate(pFmgr.uninstantiate(pFormula), pTrans.getUnprimedContext().getSsa());
    return pFormula.equals(formulaAsUnprimed);
  }

  /**
   * Checks whether pFormula is instantiated as a primed formula based on the context of the
   * transition system.
   *
   * @param pFormula The formula to be checked.
   * @param pFmgr The formula manager used for instantiation.
   * @param pTrans The transition system providing the primed ssa context.
   * @return True if pFormula is a primed formula, false otherwise.
   * @see #isUnprimed(BooleanFormula, FormulaManagerView, TransitionSystem)
   */
  public static boolean isPrimed(
      BooleanFormula pFormula, FormulaManagerView pFmgr, TransitionSystem pTrans) {
    Objects.requireNonNull(pFormula);
    Objects.requireNonNull(pFmgr);
    Objects.requireNonNull(pTrans);
    BooleanFormula formulaAsPrimed =
        pFmgr.instantiate(pFmgr.uninstantiate(pFormula), pTrans.getPrimedContext().getSsa());
    return pFormula.equals(formulaAsPrimed);
  }

  /**
   * Tries to find a block starting from the location specified in pStates to a location satisfying
   * the given filter predicate. The concrete state in pStates must satisfy the block transition.
   * Otherwise, the block isn't considered a valid candidate.
   *
   * @param pStates Specifies the starting location for the searched block and the concrete state
   *     that must satisfy the block transition.
   * @param pSuccessorSpecification The condition the block's successor location must satisfy.
   * @param pForward The stepwise transition computing the blocks.
   * @param pFmgr The formula manager used for instantiation.
   * @param pSolver The solver used for checking the satisfiability.
   * @return An Optional containing the discovered block, or an empty Optional is none was found.
   */
  public static Optional<Block> getDirectBlockToLocation(
      StatesWithLocation pStates,
      Predicate<CFANode> pSuccessorSpecification,
      ForwardTransition pForward,
      FormulaManagerView pFmgr,
      Solver pSolver)
      throws CPAException, InterruptedException, SolverException {

    FluentIterable<Block> connectingBlocks =
        pForward
            .getBlocksFrom(pStates.getLocation())
            .filter(Blocks.applyToSuccessorLocation(pSuccessorSpecification));
    if (connectingBlocks.isEmpty()) {
      return Optional.empty();
    }

    // If there is only one block to the successor location, just return that block.
    if (connectingBlocks.size() == 1) {
      return Optional.of(Iterables.getOnlyElement(connectingBlocks));
    }

    // Find the block whose formula is satisfied by the concrete state in pStates.
    for (Block b : connectingBlocks) {

      // Re-instantiate to match unprimed ssa indices of block; pc variable is still
      // present, but doesn't hurt.
      BooleanFormula reinstantiated = pFmgr.uninstantiate(pStates.getConcrete());
      reinstantiated = pFmgr.instantiate(reinstantiated, b.getUnprimedContext().getSsa());
      BooleanFormula transitionForBlock =
          pFmgr.getBooleanFormulaManager().and(reinstantiated, b.getFormula());

      if (!pSolver.isUnsat(transitionForBlock)) {
        return Optional.of(b);
      }
    }

    return Optional.empty();
  }

  /**
   * Tries to find a block from the location specified in pStates to a target-location. The concrete
   * state in pStates must satisfy the block transition.
   *
   * @param pStates Specifies the starting location for the searched block and the concrete state
   *     that must satisfy the block transition.
   * @param pTransition The transition system providing the target-locations.
   * @param pForward The stepwise transition computing the blocks.
   * @param pFmgr The formula manager used for instantiation.
   * @param pSolver The solver used for checking the satisfiability.
   * @return An Optional containing the discovered block, or an empty Optional is none was found.
   */
  public static Optional<Block> getDirectBlockToTargetLocation(
      StatesWithLocation pStates,
      TransitionSystem pTransition,
      ForwardTransition pForward,
      FormulaManagerView pFmgr,
      Solver pSolver)
      throws CPAException, InterruptedException, SolverException {
    Set<CFANode> targetLocs = Objects.requireNonNull(pTransition).getTargetLocations();
    return getDirectBlockToLocation(pStates, targetLocs::contains, pForward, pFmgr, pSolver);
  }

  /**
   * Simple utility method that performs a standard {@link BasicProverEnvironment#isUnsat()} call
   * and measures the elapsed time.
   *
   * @param pProver The prover environment that contains the formulas that should be checked for
   *     unsatisfiability.
   * @param pTimer The timer that measures the elapsed time.
   * @return True if the prover returned unsat, false otherwise.
   * @throws SolverException If the solver encountered a problem during its check.
   * @throws InterruptedException If the solving process was interrupted.
   */
  public static <T> boolean isUnsat(BasicProverEnvironment<T> pProver, Timer pTimer)
      throws SolverException, InterruptedException {
    pTimer.start();
    try {
      return pProver.isUnsat();
    } finally {
      pTimer.stop();
    }
  }

}
