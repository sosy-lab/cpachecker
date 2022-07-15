// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class TreeInterpolation extends AbstractTreeInterpolation {

  /**
   * This strategy is similar to "Tree Interpolation in Vampire*" from Blanc et al. In comparison to
   * the paper, we directly use the post-order-sorted formula-list instead of the tree. This is
   * easier to implement.
   */
  public TreeInterpolation(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, FormulaManagerView pFmgr) {
    super(pLogger, pShutdownNotifier, pFmgr);
  }

  @Override
  public <T> List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds)
      throws InterruptedException, SolverException {
    final Pair<List<Triple<BooleanFormula, AbstractState, T>>, ImmutableIntArray> p =
        buildTreeStructure(formulasWithStatesAndGroupdIds);
    final ImmutableList.Builder<BooleanFormula> itps =
        ImmutableList.builderWithExpectedSize(p.getFirst().size());
    final Deque<Pair<BooleanFormula, Integer>> itpStack = new ArrayDeque<>();
    for (int positionOfA = 0; positionOfA < p.getFirst().size() - 1; positionOfA++) {
      itps.add(
          getTreeInterpolant(interpolator, itpStack, p.getFirst(), p.getSecond(), positionOfA));
    }
    return flattenTreeItps(formulasWithStatesAndGroupdIds, itps.build());
  }

  private <T> BooleanFormula getTreeInterpolant(
      final InterpolationManager.Interpolator<T> interpolator,
      final Deque<Pair<BooleanFormula, Integer>> itpStack,
      final List<Triple<BooleanFormula, AbstractState, T>> formulas,
      final ImmutableIntArray startOfSubTree,
      final int positionOfA)
      throws SolverException, InterruptedException {

    // use a new prover, because we use several distinct interpolation-queries
    try (final InterpolatingProverEnvironment<T> itpProver = interpolator.newEnvironment()) {
      final int currentSubtree = startOfSubTree.get(positionOfA);

      // build partitions A and B
      final List<T> A = new ArrayList<>();
      final List<T> B = new ArrayList<>();
      while (!itpStack.isEmpty() && currentSubtree <= itpStack.peekLast().getSecond()) {
        A.add(itpProver.push(itpStack.pollLast().getFirst()));
      }
      A.add(itpProver.push(formulas.get(positionOfA).getFirst()));

      assert itpStack.isEmpty() == (currentSubtree == 0)
          : "empty stack is only allowed, if we are in the left-most branch"
              + startOfSubTree
              + "@"
              + positionOfA
              + "="
              + currentSubtree
              + " vs "
              + itpStack.size();

      // build partition B
      for (Pair<BooleanFormula, Integer> externalChild : itpStack) {
        B.add(itpProver.push(externalChild.getFirst()));
      }
      for (int i = positionOfA + 1; i < formulas.size(); i++) {
        B.add(itpProver.push(formulas.get(i).getFirst()));
      }

      final boolean check = itpProver.isUnsat();
      assert check : "asserted formulas should be UNSAT";

      // get interpolant via Craig interpolation
      assert !A.isEmpty() && !B.isEmpty();
      final BooleanFormula interpolant = itpProver.getInterpolant(A);

      // update the stack for further computation
      itpStack.addLast(Pair.of(interpolant, currentSubtree));
      return interpolant;
    }
  }
}
