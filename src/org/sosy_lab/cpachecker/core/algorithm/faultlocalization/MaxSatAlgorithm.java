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

  @Override
  public Set<Fault> run(FormulaContext pContext, TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, VerifyException {

    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();

    int numberSelectors = tf.traceSize();

    Set<Fault> hard = new HashSet<>();
    Set<FaultContribution> soft = new HashSet<>(tf.getSelectors());
    //if a selector is true (i. e. enabled) it cannot be part of the result set. This usually happens if the selector is a part of the pre-condition
    soft.removeIf(fc -> bmgr.isTrue(((Selector)fc).getFormula()));
    Set<Fault> singletons = new HashSet<>();

    Fault minUnsatCore = new Fault();

    totalTime.start();
    while(minUnsatCore.size() != numberSelectors){
      minUnsatCore = getMinUnsatCore(soft, tf, hard);
      /* Subsets of size 1 cannot be added to the hard set.
         Assume that pre & implicForm & S1 is unsat.
         Adding S1 to the hard set would make the formula pre & implicForm & hardSet unsat.
         No new minimal subset can be found.
         The correct handling of subsets of size 1 is to remove them from the soft set and later add
         them to the hard set. */
      if (minUnsatCore.size() == 1) {
        soft.removeAll(minUnsatCore);
        singletons.add(minUnsatCore);
        numberSelectors = soft.size();
      } else {
        //Otherwise the whole set would always be in the result set.
        if(minUnsatCore.size() == numberSelectors && singletons.size() == 0 && !hard.isEmpty()){
          break;
        }
        hard.add(minUnsatCore);
      }
    }
    totalTime.stop();

    hard.addAll(singletons);

    return hard;
  }

  /**
   * Get the minimal subset of selectors considering the already found ones
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
            pTraceFormula.getImplicationForm(),
            hardSetFormula(pHardSet));
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
        }
      }
    } while (changed);
    return new Fault(result);
  }

  private boolean isSubsetOrSupersetOf(Fault pSet, Set<Fault> pHardSet) {
    for (Set<FaultContribution> hardSet : pHardSet) {
      if (hardSet.containsAll(pSet) || pSet.containsAll(hardSet)) {
        return true;
      }
    }
    return false;
  }

  private BooleanFormula softSetFormula(Fault softSet) {
    return softSet.stream().map(f -> ((Selector)f).getFormula()).collect(bmgr.toConjunction());
  }

  /**
   * Creates the formula (a1 or a2 or a3) and (b1 or b2) ... for the input [[a1,a2,a3],[b1,b2]]
   *
   * @param hardSet the current hard set
   * @return conjunction of the disjunction of the sets
   */
  private BooleanFormula hardSetFormula(Set<Fault> hardSet) {
    return hardSet.stream()
        .map(l -> l.stream().map(f -> ((Selector)f).getFormula()).collect(bmgr.toDisjunction()))
        .collect(bmgr.toConjunction());
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
    w0.put("Total time", totalTime).put("Total calls to solver", unsatCalls);
  }

  @Override
  public @Nullable String getName() {
    return "Max-Sat algorithm";
  }
}
