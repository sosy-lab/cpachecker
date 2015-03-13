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
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Goal: Avoid irrelevant predicates.
 */
public class MinCorePrio implements InterpolationWithCandidates {

  private final CFA cfa;
  private final Solver solver;
  private final LogManager logger;
  private final FormulaManagerView mgrv;
  private final BooleanFormulaManagerView bmgr;

  public MinCorePrio(LogManager pLogger, CFA pCfa, Solver pSolver) {

    solver = Preconditions.checkNotNull(pSolver);
    logger = Preconditions.checkNotNull(pLogger);
    cfa = Preconditions.checkNotNull(pCfa);

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
      final List<BooleanFormula> pItpCandidatePredicates,
      final CFANode pItpLocation)
    throws SolverException, InterruptedException {

    Collection<BooleanFormula> resultPredicates = getInterpolantAsPredicateCollection(
        pPhiMinus, pPhiPlus, pItpCandidatePredicates, pItpLocation);

    if (resultPredicates.isEmpty()) {
      return pPhiMinus;

    }

    BooleanFormula result = bmgr.makeBoolean(true);
    for (BooleanFormula p: resultPredicates) {
      result = bmgr.and(result, p);
    }

    return result;
  }

  protected Set<BooleanFormula> predicatesOnlyOnLiveVariables(
      final Set<BooleanFormula> pFrom, final CFANode pLocation) {

    return Sets.filter(pFrom, new Predicate<BooleanFormula>() {
      @Override
      public boolean apply(BooleanFormula pArg0) {
        Set<String> predOnVars = mgrv.extractVariableNames(pArg0);
        for (String instantiatedVarName: predOnVars) {
          final String varName = FormulaManagerView.parseName(instantiatedVarName).getFirst();
          if (!cfa.getLiveVariables().get().isVariableLive(varName, pLocation)) {
            return false;
          }
        }
        return true;
      }});
  }

  @Override
  public Collection<BooleanFormula> getInterpolantAsPredicateCollection(
      final BooleanFormula pPhiMinus,
      final BooleanFormula pPhiPlus,
      final List<BooleanFormula> pItpCandidatePredicates,
      final CFANode pItpLocation)
    throws SolverException, InterruptedException {

    Preconditions.checkNotNull(pPhiMinus);
    Preconditions.checkNotNull(pPhiPlus);

    Set<BooleanFormula> resultPredicates = Sets.newHashSet(
        mgrv.extractLiterals(pPhiMinus, false, false));

    if (!isInconsistent(pPhiMinus, pPhiPlus)) {
      logger.log(Level.WARNING, "MinCorePrio: Formulas not inconsistent! The over-approximation of the system might be insufficient!");
      return resultPredicates;
      // TODO: Debug why this can be the cause. Current status: A previous interpolation process might have removed predicates that were important for the disjointness
      // throw new AssertionError("MinCorePrio: Formulas not inconsistent!");
    }

    ImmutableList<BooleanFormula> candidatesPrime = ImmutableList.<BooleanFormula>builder() // L'
      .addAll(resultPredicates) // "elements of S to L' in the front"
      .addAll(pItpCandidatePredicates) // "L' = L"
      .build();

    resultPredicates.addAll(pItpCandidatePredicates);

    for (BooleanFormula f: candidatesPrime) {

      // At least one predicate must remain as result
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
