/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class NestedInterpolation<T> extends AbstractTreeInterpolation<T> {

  /**
   * This strategy returns a sequence of interpolants.
   * It uses the callstack and previous interpolants to compute the interpolants
   * (see 'Nested Interpolants' from Heizmann, Hoenicke, and Podelski).
   * The resulting interpolants are based on a tree-like scheme.
   */
  public NestedInterpolation(LogManager pLogger, ShutdownNotifier pShutdownNotifier,
                             FormulaManagerView pFmgr, BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  @Override
  public List<BooleanFormula> getInterpolants(
          final InterpolationManager.Interpolator<T> interpolator,
          final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
          throws InterruptedException, SolverException {
    List<BooleanFormula> interpolants = Lists.newArrayListWithExpectedSize(formulasWithStatesAndGroupdIds.size() - 1);
    BooleanFormula lastItp = bfmgr.makeTrue(); // PSI_0 = True
    final Deque<Triple<BooleanFormula,BooleanFormula,CFANode>> callstack = new ArrayDeque<>();
    for (int positionOfA = 0; positionOfA < formulasWithStatesAndGroupdIds.size() - 1; positionOfA++) {
      // use a new prover, because we use several distinct queries
      lastItp = getNestedInterpolant(formulasWithStatesAndGroupdIds, interpolants, callstack, interpolator, positionOfA, lastItp);
    }
    return interpolants;
  }


  /** This function implements the paper "Nested Interpolants" with a small modification:
   * instead of a return-edge, we use dummy-edges with simple pathformula "true".
   * Actually the implementation does not use "true", but omits it completely and
   * returns the conjunction of the two interpolants (before and after the (non-existing) dummy edge).
   * TODO simplify this algorithm, it is soo ugly! Maybe it is 'equal' with the normal tree-interpolation. */
  private BooleanFormula getNestedInterpolant(
          final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
          final List<BooleanFormula> interpolants,
          final Deque<Triple<BooleanFormula, BooleanFormula, CFANode>> callstack,
          final InterpolationManager.Interpolator<T> interpolator,
          int positionOfA, BooleanFormula lastItp)
              throws InterruptedException, SolverException {

    // use a new prover, because we use several distinct queries
    try (final InterpolatingProverEnvironment<T> itpProver = interpolator.newEnvironment()) {

      final List<T> A = new ArrayList<>();

      // If we have entered or exited a function, update the stack of entry points
      final AbstractState abstractionState = checkNotNull(formulasWithStatesAndGroupdIds.get(positionOfA).getSecond());
      final CFANode node = AbstractStates.extractLocation(abstractionState);

      if (node instanceof FunctionEntryNode && callHasReturn(formulasWithStatesAndGroupdIds, positionOfA)) {
        // && (positionOfA > 0)) {
        // case 2 from paper
        final BooleanFormula call = formulasWithStatesAndGroupdIds.get(positionOfA).getFirst();
        callstack.addLast(Triple.of(lastItp, call, node));
        final BooleanFormula itp = bfmgr.makeTrue();
        interpolants.add(itp);
        return itp; // PSIminus = True --> PSI = True, for the 3rd rule ITP is True
      }

      A.add(itpProver.push(lastItp));
      A.add(itpProver.push(formulasWithStatesAndGroupdIds.get(positionOfA).getFirst()));

      // add all remaining PHI_j
      for (Triple<BooleanFormula, AbstractState, T> t : Iterables.skip(formulasWithStatesAndGroupdIds, positionOfA + 1)) {
        itpProver.push(t.getFirst());
      }

      // add all previous function calls
      for (Triple<BooleanFormula,BooleanFormula, CFANode> t : callstack) {
        itpProver.push(t.getFirst()); // add PSI_k
        itpProver.push(t.getSecond()); // ... and PHI_k
      }

      // update prover with new formulas.
      // this is the expensive step, that is distinct from other strategies.
      // TODO improve! example: reverse ordering of formulas for re-usage of the solver-stack
      boolean unsat = itpProver.isUnsat();
      assert unsat : "formulas were unsat before, they have to be unsat now.";

      // get interpolant of A and B, for B we use the complementary set of A
      final BooleanFormula itp = itpProver.getInterpolant(A);

      if (!callstack.isEmpty() && node instanceof FunctionExitNode) {
        // case 4, we are returning from a function, rule 4
        Triple<BooleanFormula, BooleanFormula, CFANode> scopingItp = callstack.removeLast();

        try (InterpolatingProverEnvironment<T> itpProver2 = interpolator.newEnvironment()) {
          final List<T> A2 = new ArrayList<>();

          A2.add(itpProver2.push(itp));
          //A2.add(itpProver2.push(orderedFormulas.get(positionOfA).getFirst()));

          A2.add(itpProver2.push(scopingItp.getFirst()));
          A2.add(itpProver2.push(scopingItp.getSecond()));

          // add all remaining PHI_j
          for (Triple<BooleanFormula, AbstractState, T> t :
              Iterables.skip(formulasWithStatesAndGroupdIds, positionOfA + 1)) {
            itpProver2.push(t.getFirst());
          }

          // add all previous function calls
          for (Triple<BooleanFormula, BooleanFormula, CFANode> t : callstack) {
            itpProver2.push(t.getFirst()); // add PSI_k
            itpProver2.push(t.getSecond()); // ... and PHI_k
          }

          boolean unsat2 = itpProver2.isUnsat();
          assert unsat2 : "formulas2 were unsat before, they have to be unsat now.";

          // get interpolant of A and B, for B we use the complementary set of A
          BooleanFormula itp2 = itpProver2.getInterpolant(A2);

          BooleanFormula rebuildItp = rebuildInterpolant(itp, itp2);
          if (!bfmgr.isTrue(scopingItp.getFirst())) {
            rebuildItp = bfmgr.and(rebuildItp, scopingItp.getFirst());
          }

          interpolants.add(rebuildItp);
          return itp2;
        }

      } else {
        interpolants.add(itp);
        return itp;
      }
    }
  }
}
