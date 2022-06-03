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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
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

public class OriginalMaxSatAlgorithm implements FaultLocalizerWithTraceFormula, StatisticsProvider {

  // Statistics
  private final MaxSatStatistics stats = new MaxSatStatistics();

  @Override
  public Set<Fault> run(FormulaContext pContext, TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, VerifyException {

    Solver solver = pContext.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula booleanTraceFormula = tf.toFormula(new SelectorTraceInterpreter(bmgr), true);

    Set<Fault> hard = new LinkedHashSet<>();

    // if selectors are reduced the set ensures to remove duplicates
    Set<TraceAtom> soft = new LinkedHashSet<>(tf.getTrace());
    // if a selector is true (i. e. enabled) it cannot be part of the result set. This usually
    // happens if the selector is a part of the pre-condition
    soft.removeIf(fc -> bmgr.isTrue(fc.getFormula()) || bmgr.isFalse(fc.getFormula()));

    Set<TraceAtom> complement;
    stats.totalTime.start();
    // loop as long as new maxsat cores are found.
    while (true) {
      complement = coMSS(soft, hard, booleanTraceFormula, pContext);
      if (complement.isEmpty()) {
        break;
      }
      hard.add(
          FluentIterable.from(complement)
              .transform(atom -> (FaultContribution) atom)
              .copyInto(new Fault()));
      soft.removeAll(complement);
    }
    stats.totalTime.stop();
    return hard;
  }

  /**
   * Get the complement of a maximal satisfiable set considering the already found ones
   *
   * @param pTraceFormula TraceFormula to the error
   * @param pHardSet already found minimal sets
   * @return new minimal set
   * @throws SolverException thrown if tf is satisfiable
   * @throws InterruptedException thrown if interrupted
   */
  private Set<TraceAtom> coMSS(
      Set<TraceAtom> pSoftSet,
      Set<Fault> pHardSet,
      BooleanFormula pTraceFormula,
      FormulaContext pContext)
      throws SolverException, InterruptedException {
    Solver solver = pContext.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    Set<TraceAtom> selectors = new LinkedHashSet<>(pSoftSet);
    Fault result = new Fault();
    BooleanFormula composedFormula = bmgr.and(pTraceFormula, hardSetFormula(pHardSet, bmgr));
    boolean changed;
    do {
      changed = false;
      for (TraceAtom atom : selectors) {
        Fault copy = new Fault(result);
        copy.add(atom);
        stats.unsatCalls.inc();
        if (!solver.isUnsat(bmgr.and(composedFormula, softSetFormula(copy, bmgr)))) {
          changed = true;
          result.add(atom);
          selectors.remove(atom);
          break;
        }
      }
    } while (changed);
    return selectors;
  }

  /**
   * Conjunct of all selector-formulas
   *
   * @param softSet left selectors
   * @return boolean formula as conjunct of all selector formulas
   */
  private BooleanFormula softSetFormula(Fault softSet, BooleanFormulaManager bmgr) {
    return softSet.stream().map(f -> ((TraceAtom) f).getFormula()).collect(bmgr.toConjunction());
  }

  /**
   * Creates the formula (a1 or a2 or a3) and (b1 or b2) ... for the input [[a1,a2,a3],[b1,b2]]
   *
   * @param hardSet the current hard set
   * @return conjunction of the disjunction of the sets
   */
  private BooleanFormula hardSetFormula(Set<Fault> hardSet, BooleanFormulaManager bmgr) {
    return hardSet.stream()
        .map(l -> l.stream().map(f -> ((TraceAtom) f).getFormula()).collect(bmgr.toDisjunction()))
        .collect(bmgr.toConjunction());
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}
