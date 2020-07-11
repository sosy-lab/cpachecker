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
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class MaxSatAlgorithm implements FaultLocalizationAlgorithmInterface, Statistics {

  private Solver solver;
  private BooleanFormulaManager bmgr;

  //Statistics
  private StatTimer totalTime = new StatTimer(StatKind.SUM, "Total time to find all subsets");
  private StatCounter unsatCalls = new StatCounter("Total calls to sat solver");
  private StatCounter savedCalls = new StatCounter("Total calls prevented by subset check");
  @Override
  public Set<Fault> run(FormulaContext pContext, TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, VerifyException {

    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();

    Set<Fault> hard = new HashSet<>();

    Set<FaultContribution> soft = new HashSet<>(tf.getRelevantSelectors());
    //if a selector is true (i. e. enabled) it cannot be part of the result set. This usually happens if the selector is a part of the pre-condition
    soft.removeIf(fc -> bmgr.isTrue(((Selector)fc).getFormula()) || bmgr.isFalse(((Selector)fc).getFormula()));
    int numberSelectors = soft.size();

    Fault minUnsatCore = new Fault();

    totalTime.start();
    // loop as long as new unsat cores are found.
    // if the newly found unsat core has the size of all left selectors break.
    while(minUnsatCore.size() != numberSelectors){
      minUnsatCore = getMinUnsatCore(soft, tf, hard);
      if (minUnsatCore.size() == 1) {
        soft.removeAll(minUnsatCore);
        numberSelectors = soft.size();
      }
      // adding all possible selectors yields no information because the user knows that the program
      // is buggy
      if(minUnsatCore.size() != tf.getRelevantSelectors().size()) {
        hard.add(minUnsatCore);
      }
    }
    totalTime.stop();
    return hard;
  }

  /**
   * Get a minimal subset of selectors considering the already found ones
   * Minimal means that we cannot remove a single selector from the returned set and maintain unsatisfiability.
   * Minimal does not mean that there does not exist a smaller unsat-core here.
   * Since we find all solutions the order does not matter.
   *
   * @param pTraceFormula TraceFormula to the error
   * @param pHardSet already found minimal sets
   * @return new minimal set
   * @throws SolverException thrown if tf is satisfiable
   * @throws InterruptedException thrown if interrupted
   */
  private Fault getMinUnsatCore(
      Set<FaultContribution> pSoftSet, TraceFormula pTraceFormula, Set<Fault> pHardSet)
      throws SolverException, InterruptedException {
    Fault result = new Fault(new HashSet<>(pSoftSet));
    BooleanFormula composedFormula =
        bmgr.and(
            pTraceFormula.getPreCondition(),
            pTraceFormula.getImplicationForm());
    boolean changed;
    do {
      changed = false;
      for (FaultContribution fc : result) {
        Selector s = (Selector)fc;
        Fault copy = new Fault(new HashSet<>(result));
        copy.remove(s);
        if (!isSubsetOrSupersetOf(copy, pHardSet)) {
          unsatCalls.inc();
          if (solver.isUnsat(bmgr.and(composedFormula, softSetFormula(copy)))) {
            changed = true;
            result.remove(s);
            break;
          }
        } else {
          savedCalls.inc();
        }
      }
    } while (changed);
    return result;
  }

  private boolean isSubsetOrSupersetOf(Fault pSet, Set<Fault> pHardSet) {
    for (Set<FaultContribution> hardSet : pHardSet) {
      if (hardSet.containsAll(pSet) || pSet.containsAll(hardSet)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Conjunct of all selector-formulas
   *
   * @param softSet left selectors
   * @return boolean formula as conjunct of all selector formulas
   */
  private BooleanFormula softSetFormula(Fault softSet) {
    return softSet.stream().map(f -> ((Selector) f).getFormula()).collect(bmgr.toConjunction());
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
    w0.put("Total time", totalTime).put("Total calls to solver", unsatCalls)
    .put("Total calls saved", savedCalls);
  }

  @Override
  public @Nullable String getName() {
    return "MAX-SAT algorithm";
  }
}
