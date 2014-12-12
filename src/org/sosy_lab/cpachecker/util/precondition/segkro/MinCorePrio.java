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

import static org.sosy_lab.cpachecker.util.precondition.segkro.FormulaUtils.substractEqualFromulasFrom;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Goal: Avoid irrelevant predicates.
 */
public class MinCorePrio implements InterpolationWithCandidates {

  private final FormulaManager mgr;
  private final FormulaManagerView mgrv;
  private final Solver solver;
  private final BooleanFormulaManagerView bmgr;

  public MinCorePrio(FormulaManager pMgr, FormulaManagerView pMgrv, Solver pSolver) {
    super();
    mgr = pMgr;
    mgrv = pMgrv;
    solver = pSolver;
    bmgr = mgrv.getBooleanFormulaManager();
  }

  private boolean isInconsistent(BooleanFormula pF1, BooleanFormula pF2) throws SolverException, InterruptedException {
    return solver.isUnsat(bmgr.and(pF1, pF2));
  }

  /**
   * Computes a minimal set of predicates that describes pConjunction
   * so that it is still inconsistent with pArbitraryFormula.
   *    (Craig interpolant property)
   *
   * @param pConjunction
   * @param pArbitraryFormula
   * @param pCandidates
   * @throws InterruptedException
   * @throws SolverException
   */
  @Override
  public BooleanFormula getInterpolant(BooleanFormula pConjunction, BooleanFormula pArbitraryFormula, List<BooleanFormula> pCandidates)
      throws SolverException, InterruptedException {

    Preconditions.checkArgument(isInconsistent(pConjunction, pArbitraryFormula));

    Collection<BooleanFormula> resultPredicates = mgrv.extractLiterals(pConjunction, false, false);
    List<BooleanFormula> candidatesPrime = Lists.newLinkedList();

    candidatesPrime.addAll(resultPredicates);
    candidatesPrime.addAll(pCandidates);
    resultPredicates.addAll(pCandidates);

    for (BooleanFormula f: candidatesPrime) {
      // ...
      // TODO: It might be sufficient to substract based of the identity

      // At least one predicate must remain as result
      List<BooleanFormula> resultPredicatesMinusF = substractEqualFromulasFrom(
          resultPredicates,
          Collections.<BooleanFormula>singleton(f));
      if (resultPredicatesMinusF.isEmpty()) {
        return f;
      }

      // Check if removing the predicate from the set 'resultpredicates'
      //    maintains the inconsistency with pArbitraryFormula
      boolean stillInconsistent = isInconsistent(
          bmgr.and(resultPredicatesMinusF),
          pArbitraryFormula);

      if (stillInconsistent) {
        // Remove the predicate if it does not contribute to the inconsistency
        resultPredicates.remove(f);
      }
    }

    // The result is the conjunction of the remaining predicates
    BooleanFormula result = bmgr.makeBoolean(true);
    for (BooleanFormula p: resultPredicates) {
      result = bmgr.and(result, p);
    }
    return result;
  }
}
