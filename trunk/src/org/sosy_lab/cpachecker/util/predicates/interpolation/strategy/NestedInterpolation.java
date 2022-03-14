// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class NestedInterpolation extends AbstractTreeInterpolation {

  /**
   * This strategy returns a sequence of interpolants. It uses the callstack and previous
   * interpolants to compute the interpolants (see 'Nested Interpolants' from Heizmann, Hoenicke,
   * and Podelski). The resulting interpolants are based on a tree-like scheme.
   */
  public NestedInterpolation(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, FormulaManagerView pFmgr) {
    super(pLogger, pShutdownNotifier, pFmgr);
  }

  @Override
  public <T> List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
      throws InterruptedException, SolverException {
    final ImmutableList.Builder<BooleanFormula> interpolants =
        ImmutableList.builderWithExpectedSize(formulasWithStatesAndGroupdIds.size() - 1);
    BooleanFormula lastItp = bfmgr.makeTrue(); // PSI_0 = True
    final Deque<Pair<BooleanFormula, BooleanFormula>> callstack = new ArrayDeque<>();
    for (int positionOfA = 0;
        positionOfA < formulasWithStatesAndGroupdIds.size() - 1;
        positionOfA++) {
      // use a new prover, because we use several distinct queries
      lastItp =
          getNestedInterpolant(
              formulasWithStatesAndGroupdIds,
              interpolants,
              callstack,
              interpolator,
              positionOfA,
              lastItp);
    }
    final ImmutableList<BooleanFormula> result = interpolants.build();
    assert formulasWithStatesAndGroupdIds.size() == result.size() + 1;
    if (!result.isEmpty()) {
      assert lastItp == Iterables.getLast(result);
    } // else: single block with unsatisfiable path formula -> no interpolant
    return result;
  }

  /**
   * This function implements the paper "Nested Interpolants" with a small modification: instead of
   * a return-edge, we use dummy-edges with simple pathformula "true". Actually the implementation
   * does not use "true", but omits it completely and returns the conjunction of the two
   * interpolants (before and after the (non-existing) dummy edge). TODO simplify this algorithm, it
   * is soo ugly! Maybe it is 'equal' with the normal tree-interpolation.
   */
  private <T> BooleanFormula getNestedInterpolant(
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
      final ImmutableList.Builder<BooleanFormula> interpolants,
      final Deque<Pair<BooleanFormula, BooleanFormula>> callstack,
      final InterpolationManager.Interpolator<T> interpolator,
      final int positionOfA,
      final BooleanFormula lastItp)
      throws InterruptedException, SolverException {

    final AbstractState abstractionState =
        checkNotNull(formulasWithStatesAndGroupdIds.get(positionOfA).getSecond());
    final CFANode node = AbstractStates.extractLocation(abstractionState);

    // If we have entered or exited a function, update the stack of entry points
    if (node instanceof FunctionEntryNode
        && callHasReturn(formulasWithStatesAndGroupdIds, positionOfA)) {
      // && (positionOfA > 0)) {
      // case 2 from paper
      final BooleanFormula call = formulasWithStatesAndGroupdIds.get(positionOfA).getFirst();
      callstack.addLast(Pair.of(lastItp, call));
      final BooleanFormula itpTrue = bfmgr.makeTrue();
      interpolants.add(itpTrue);
      return itpTrue; // PSIminus = True --> PSI = True, for the 3rd rule ITP is True
    }

    final BooleanFormula itp;
    // use a new prover, because we use several distinct queries
    try (final InterpolatingProverEnvironment<T> itpProver = interpolator.newEnvironment()) {
      final List<T> A = new ArrayList<>();
      final List<T> B = new ArrayList<>();

      A.add(itpProver.push(lastItp));
      A.add(itpProver.push(formulasWithStatesAndGroupdIds.get(positionOfA).getFirst()));

      // add all remaining PHI_j
      for (Triple<BooleanFormula, AbstractState, T> t :
          Iterables.skip(formulasWithStatesAndGroupdIds, positionOfA + 1)) {
        B.add(itpProver.push(t.getFirst()));
      }

      // add all previous function calls
      for (Pair<BooleanFormula, BooleanFormula> t : callstack) {
        B.add(itpProver.push(t.getFirst())); // add PSI_k
        B.add(itpProver.push(t.getSecond())); // ... and PHI_k
      }

      // update prover with new formulas.
      // this is the expensive step, that is distinct from other strategies.
      // TODO improve! example: reverse ordering of formulas for re-usage of the solver-stack
      boolean unsat = itpProver.isUnsat();
      assert unsat : "formulas were unsat before, they have to be unsat now.";

      // get interpolant of A and B, for B we use the complementary set of A
      assert !A.isEmpty() && !B.isEmpty();
      itp = itpProver.getInterpolant(A);
    }

    if (!callstack.isEmpty() && node instanceof FunctionExitNode) {
      // case 4, we are returning from a function, rule 4
      Pair<BooleanFormula, BooleanFormula> scopingItp = callstack.removeLast();

      try (InterpolatingProverEnvironment<T> itpProver2 = interpolator.newEnvironment()) {
        final List<T> A2 = new ArrayList<>();
        final List<T> B2 = new ArrayList<>();

        A2.add(itpProver2.push(itp));
        // A2.add(itpProver2.push(orderedFormulas.get(positionOfA).getFirst()));

        A2.add(itpProver2.push(scopingItp.getFirst()));
        A2.add(itpProver2.push(scopingItp.getSecond()));

        // add all remaining PHI_j
        for (Triple<BooleanFormula, AbstractState, T> t :
            Iterables.skip(formulasWithStatesAndGroupdIds, positionOfA + 1)) {
          B2.add(itpProver2.push(t.getFirst()));
        }

        // add all previous function calls
        for (Pair<BooleanFormula, BooleanFormula> t : callstack) {
          B2.add(itpProver2.push(t.getFirst())); // add PSI_k
          B2.add(itpProver2.push(t.getSecond())); // ... and PHI_k
        }

        boolean unsat2 = itpProver2.isUnsat();
        assert unsat2 : "formulas2 were unsat before, they have to be unsat now.";

        // get interpolant of A2 and B2, for B2 we use the complementary set of A2
        assert !A2.isEmpty() && !B2.isEmpty();
        BooleanFormula itp2 = itpProver2.getInterpolant(A2);

        BooleanFormula rebuildItp = rebuildInterpolant(itp, itp2);
        BooleanFormula scope = scopingItp.getFirst();

        // filter out FALSE, because it might be simplified, and we need the atoms of the formula.
        // We ignore the case when the rebuildItp is FALSE, because the analysis will first
        // compute the nested abstraction, which (in theory) uses the rebuildItp's atoms.
        if (!bfmgr.isFalse(scope)) {
          rebuildItp = bfmgr.and(rebuildItp, scope);
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
