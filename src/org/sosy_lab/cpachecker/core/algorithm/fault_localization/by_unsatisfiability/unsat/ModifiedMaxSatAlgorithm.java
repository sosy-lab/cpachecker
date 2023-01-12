// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat;

import com.google.common.base.VerifyException;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.SelectorTraceInterpreter;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace.TraceAtom;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class ModifiedMaxSatAlgorithm implements FaultLocalizerWithTraceFormula, StatisticsProvider {

  // Statistics
  private final MaxSatStatistics stats = new MaxSatStatistics();

  @Override
  public Set<Fault> run(FormulaContext pContext, TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, VerifyException {

    Solver solver = pContext.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula booleanTraceFormula = tf.toFormula(new SelectorTraceInterpreter(bmgr), true);

    Set<Set<TraceAtom>> hard = new LinkedHashSet<>();

    Set<TraceAtom> soft = new LinkedHashSet<>(tf.getTrace());
    int initSize = soft.size();

    Set<TraceAtom> minUnsatCore = new LinkedHashSet<>();

    stats.totalTime.start();
    // loop as long as new unsat cores are found.
    // if the newly found unsat core has the size of all left selectors break.
    while (minUnsatCore.size() != initSize) {
      minUnsatCore = getMinUnsatCore(soft, hard, booleanTraceFormula, pContext);
      if (minUnsatCore.size() == 1) {
        soft.removeAll(minUnsatCore);
        initSize = soft.size();
      }
      // adding all possible selectors yields no information because the user knows that the program
      // has bugs
      if (minUnsatCore.size() != initSize) {
        hard.add(minUnsatCore);
      }
    }
    stats.totalTime.stop();
    pContext
        .getLogger()
        .log(
            Level.FINEST,
            "tfresult="
                + FluentIterable.from(hard)
                    .transformAndConcat(ImmutableList::copyOf)
                    .transform(
                        fc -> fc.correspondingEdge().getFileLocation().getStartingLineInOrigin())
                    .toSortedList(Integer::compareTo));
    return Collections3.transformedImmutableSetCopy(
        hard,
        h ->
            FluentIterable.from(h)
                .transform(atom -> (FaultContribution) atom)
                .copyInto(new Fault()));
  }

  /**
   * Get a minimal subset of selectors considering the already found ones Minimal means that we
   * cannot remove a single selector from the returned set and maintain unsatisfiability. Minimal
   * does not mean that there does not exist a smaller unsat-core here. Since we find all solutions
   * the order does not matter.
   *
   * @param pTraceFormula TraceFormula to the error
   * @param pHardSet already found minimal sets
   * @return new minimal set
   * @throws SolverException thrown if tf is satisfiable
   * @throws InterruptedException thrown if interrupted
   */
  private Set<TraceAtom> getMinUnsatCore(
      Set<TraceAtom> pSoftSet,
      Set<Set<TraceAtom>> pHardSet,
      BooleanFormula pTraceFormula,
      FormulaContext pContext)
      throws SolverException, InterruptedException {
    Solver solver = pContext.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    Set<TraceAtom> result = new LinkedHashSet<>(pSoftSet);
    boolean changed;
    do {
      changed = false;
      for (TraceAtom atom : result) {
        Set<TraceAtom> copy = new LinkedHashSet<>(result);
        copy.remove(atom);
        if (!isSubsetOrSupersetOf(copy, pHardSet)) {
          stats.unsatCalls.inc();
          if (solver.isUnsat(bmgr.and(pTraceFormula, softSetFormula(copy, bmgr)))) {
            changed = true;
            result.remove(atom);
            break;
          }
        } else {
          stats.savedCalls.inc();
        }
      }
    } while (changed);
    return result;
  }

  private boolean isSubsetOrSupersetOf(Set<TraceAtom> pSet, Set<Set<TraceAtom>> pHardSet) {
    stats.timeForSubSupCheck.start();
    try {
      for (Set<TraceAtom> hardSet : pHardSet) {
        if (hardSet.containsAll(pSet) || pSet.containsAll(hardSet)) {
          return true;
        }
      }
      return false;
    } finally {
      stats.timeForSubSupCheck.stop();
    }
  }

  /**
   * Conjunct of all selector-formulas
   *
   * @param softSet left selectors
   * @return boolean formula as conjunct of all selector formulas
   */
  private BooleanFormula softSetFormula(Set<TraceAtom> softSet, BooleanFormulaManager bmgr) {
    return softSet.stream().map(f -> f.getSelector()).collect(bmgr.toConjunction());
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}
