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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Goal: Avoid irrelevant predicates.
 */
public class MinCorePrio implements InterpolationWithCandidates {

  private final FormulaManagerView mgrv;
  private final Solver solver;
  private final BooleanFormulaManagerView bmgr;

  public MinCorePrio(Solver pSolver) {
    solver = pSolver;
    mgrv = pSolver.getFormulaManager();
    bmgr = mgrv.getBooleanFormulaManager();
  }

  private boolean isInconsistent(BooleanFormula pF1, BooleanFormula pF2)
      throws SolverException, InterruptedException {

    return solver.isUnsat(bmgr.and(pF1, pF2));
  }

  /**
   * Computes a minimal set of predicates that describes pPhiMinus
   * so that it is still inconsistent with pPhiPlus.
   *    (Craig interpolant property)
   *
   *  The list pItpCandidatePredicates is sorted in ascending order according
   *    the priority of its elements; predicates with lower priority are in front.
   *
   * @param pPhiMinus
   * @param pPhiPlus
   * @param pItpCandidatePredicates
   *
   * @throws InterruptedException
   * @throws SolverException
   */
  @Override
  public BooleanFormula getInterpolant(
      final BooleanFormula pPhiMinus,
      final BooleanFormula pPhiPlus,
      final List<BooleanFormula> pItpCandidatePredicates)
    throws SolverException, InterruptedException {

    Collection<BooleanFormula> resultPredicates = getInterpolantAsPredicateCollection(
        pPhiMinus, pPhiPlus, pItpCandidatePredicates);

    // The result is the conjunction of the predicates
    BooleanFormula result = bmgr.makeBoolean(true);
    for (BooleanFormula p: resultPredicates) {
      result = bmgr.and(result, p);
    }

    return result;
  }

  @Override
  public Collection<BooleanFormula> getInterpolantAsPredicateCollection(
      final BooleanFormula pPhiMinus,
      final BooleanFormula pPhiPlus,
      final List<BooleanFormula> pItpCandidatePredicates)
    throws SolverException, InterruptedException {

    Preconditions.checkNotNull(pPhiMinus);
    Preconditions.checkNotNull(pPhiPlus);

    if (!isInconsistent(pPhiMinus, pPhiPlus)) {
      throw new AssertionError("MinCorePrio: Formulas not inconsistent!");
    }

    // ATTENTION: the following line might be different from the paper! Literals vs. atoms!
    Set<BooleanFormula> resultPredicates = Sets.newHashSet(
        mgrv.extractLiterals(pPhiMinus, false, false, false));

    ImmutableList<BooleanFormula> candidatesPrime = ImmutableList.<BooleanFormula>builder()
      .addAll(resultPredicates) // "elements of S to L' in the front"
      .addAll(pItpCandidatePredicates) // "L' = L"
      .build();

    resultPredicates.addAll(pItpCandidatePredicates);

    for (BooleanFormula f: candidatesPrime) {
      // ...
      // TODO: It might be sufficient to substract based of the identity

      // At least one predicate must remain as result
      // CHANGED COMPARED TO MinCorePrio in to original paper!!!!! List instead of Set!!
      Set<BooleanFormula> resultPredicatesMinusF = Sets.newHashSet(resultPredicates);
      resultPredicatesMinusF.remove(f);
      if (resultPredicatesMinusF.isEmpty()) {
        return resultPredicates;
      }

      // Check if removing the predicate from the set 'resultpredicates'
      //    maintains the inconsistency with pArbitraryFormula
      boolean stillInconsistent = isInconsistent(
          bmgr.and(Lists.newArrayList(resultPredicatesMinusF)),
          pPhiPlus);

      if (stillInconsistent) {
        // Remove the predicate if it does not contribute to the inconsistency
        resultPredicates.remove(f);
      }
    }

    // The result is the conjunction/collection of the remaining predicates
    return resultPredicates;
  }
}
