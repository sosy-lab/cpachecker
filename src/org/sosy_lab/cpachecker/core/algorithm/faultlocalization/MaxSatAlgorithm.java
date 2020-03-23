/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization;

import com.google.common.base.VerifyException;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class MaxSatAlgorithm implements FaultLocalizationAlgorithmInterface {

  private Solver solver;
  private BooleanFormulaManager bmgr;

  public ErrorIndicatorSet<Selector> run(FormulaContext pContext, TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, VerifyException {

    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();

    int numberSelectors = tf.getSelectors().size();

    Set<Set<Selector>> hard = new HashSet<>();
    Set<Selector> soft = new HashSet<>(tf.getSelectors());
    Set<Set<Selector>> singletons = new HashSet<>();

    Set<Selector> add = getMinUnsatCore(soft, tf, hard);

    if (add.size() == numberSelectors) {
      hard.add(add);
      return new ErrorIndicatorSet<>(hard);
    }

    while (add.size() != numberSelectors) {
      // Subsets of size 1 cannot be added to the hard set.
      // Assume that pre & implicForm & S1 is unsat.
      // Adding S1 to the hard set would make the formula pre & implicForm & hardSet unsat.
      // No new minimal subset can be found.
      // The correct handling of subsets of size 1 is to remove them from the soft set and later add
      // them to the hard set.
      if (add.size() == 1) {
        soft.removeAll(add);
        singletons.add(add);
        numberSelectors = soft.size();
      } else {
        hard.add(add);
      }
      add = getMinUnsatCore(soft, tf, hard);
      // Very small programs lead to problems in analyzing.
      // Imagine the program: int a = 0;
      if (add.size() == numberSelectors
          && hard.stream().map(l -> l.size() == 1).reduce((a, b) -> a && b).orElse(false)) {
        hard.add(add);
      }
    }

    hard.addAll(singletons);

    return new ErrorIndicatorSet<>(hard);
  }

  /**
   * get the minimal subset of selectors considering the already found ones
   *
   * @param pTraceFormula TraceFormula to the error
   * @param pHardSet already found minimal sets
   * @return new minimal set
   * @throws SolverException thrown if tf is satisfiable
   * @throws InterruptedException thrown if interrupted
   */
  private Set<Selector> getMinUnsatCore(
      Set<Selector> pSoftSet, TraceFormula pTraceFormula, Set<Set<Selector>> pHardSet)
      throws SolverException, InterruptedException {
    Set<Selector> result = new HashSet<>(pSoftSet);
    BooleanFormula composedFormula =
        bmgr.and(
            pTraceFormula.getPreCondition(),
            pTraceFormula.getImplicationForm(),
            hardSetFormula(pHardSet));
    boolean changed;
    do {
      changed = false;
      for (Selector s : result) {
        Set<Selector> copy = new HashSet<>(result);
        copy.remove(s);
        if (!isSubsetOrSupersetOf(copy, pHardSet)) {
          if (solver.isUnsat(bmgr.and(composedFormula, softSetFormula(copy)))) {
            changed = true;
            result.remove(s);
            break;
          }
        }
      }
    } while (changed);
    return result;
  }

  private boolean isSubsetOrSupersetOf(Set<Selector> pSet, Set<Set<Selector>> pHardSet) {
    for (Set<Selector> hardSet : pHardSet) {
      if (hardSet.containsAll(pSet) || pSet.containsAll(hardSet)) {
        return true;
      }
    }
    return false;
  }

  private BooleanFormula softSetFormula(Set<Selector> softSet) {
    return softSet.stream().map(Selector::getFormula).collect(bmgr.toConjunction());
  }

  /**
   * Creates the formula (a1 or a2 or a3) and (b1 or b2) ... for the input [[a1,a2,a3],[b1,b2]]
   *
   * @param hardSet the current hard set
   * @return conjunction of the disjunction of the sets
   */
  private BooleanFormula hardSetFormula(Set<Set<Selector>> hardSet) {
    return hardSet.stream()
        .map(l -> l.stream().map(Selector::getFormula).collect(bmgr.toDisjunction()))
        .collect(bmgr.toConjunction());
  }
}
