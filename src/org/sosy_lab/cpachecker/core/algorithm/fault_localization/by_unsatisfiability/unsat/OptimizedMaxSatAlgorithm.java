// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace.TraceAtom;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class OptimizedMaxSatAlgorithm
    implements FaultLocalizerWithTraceFormula, StatisticsProvider {

  // Statistics
  private final MaxSatStatistics stats = new MaxSatStatistics();

  private final boolean stopAfterFirstFault;

  public OptimizedMaxSatAlgorithm(boolean pStopAfterFirstFault) {
    stopAfterFirstFault = pStopAfterFirstFault;
  }

  @Override
  public Set<Fault> run(FormulaContext pContext, TraceFormula tf)
      throws SolverException, CPAException {

    Solver solver = pContext.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    TraceFormula booleanTraceFormula = tf;

    Set<Fault> hard = new LinkedHashSet<>();
    // if selectors are reduced the set ensures to remove duplicates
    Set<TraceAtom> soft = new LinkedHashSet<>(tf.getTrace());
    // if a selector is true (i.e. enabled) it cannot be part of the result set. This usually
    // happens if the selector is a part of the pre-condition
    soft.removeIf(fc -> bmgr.isTrue(fc.getFormula()) || bmgr.isFalse(fc.getFormula()));

    Set<TraceAtom> complement;
    Set<TraceAtom> softCopy = new LinkedHashSet<>(soft);

    // Save already found MSS in foundMSS
    Set<Fault> foundMSS = new LinkedHashSet<>();
    stats.totalTime.start();
    // loop as long as new maxsat cores are found.
    try {

      while (true) {
        softCopy = new LinkedHashSet<>(soft);
        if (pContext.getShutdownNotifier().shouldShutdown()) {
          break;
        }
        complement = coMSS(soft, hard, booleanTraceFormula, pContext, foundMSS);

        if (complement.isEmpty()) {
          break;
        }

        // Save already found MSS as Fault in foundMSS
        // -> softCopy without complement is MSS
        Fault foundMSSFault = new Fault();
        softCopy.removeAll(complement);
        foundMSSFault.addAll(
            FluentIterable.from(softCopy).transform(atom -> (FaultContribution) atom).toList());

        if (foundMSSFault.size() == 0 && foundMSS.size() > 0) {
          break;
        }

        foundMSS.add(foundMSSFault);

        hard.add(
            FluentIterable.from(complement)
                .transform(atom -> (FaultContribution) atom)
                .copyInto(new Fault()));

        if (stopAfterFirstFault) {
          break;
        }
      }
    } catch (InterruptedException e) {
      pContext
          .getLogger()
          .logfException(Level.WARNING, e, "Stopping fault localization after interruption.");
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Invalid configuration of trace formula", e);
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
   * @throws InvalidConfigurationException
   * @throws CPATransferException
   */
  private Set<TraceAtom> coMSS(
      Set<TraceAtom> pSoftSet,
      Set<Fault> pHardSet,
      TraceFormula pTraceFormula,
      FormulaContext pContext,
      Set<Fault> pFoundMSS)
      throws SolverException,
          InterruptedException,
          CPATransferException,
          InvalidConfigurationException {
    Solver solver = pContext.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    Set<TraceAtom> selectors = new LinkedHashSet<>(pSoftSet);
    Fault result = new Fault();

    SSAMap newSSAMap = pTraceFormula.getTrace().getInitialSsaMap();
    SSAMapBuilder newBuilder = SSAMap.emptySSAMap().builder();
    for (String variable : newSSAMap.allVariables()) {
      newBuilder.setIndex(variable, newSSAMap.getType(variable), 1);
    }
    BooleanFormula preCondition =
        pTraceFormula
            .getPrecondition()
            .instantiate(solver.getFormulaManager(), newBuilder.build())
            .getPrecondition();

    boolean changed;
    do {
      changed = false;
      for (TraceAtom atom : selectors) {
        Fault copy = new Fault(result);
        copy.add(atom);
        // skip if the new MSS is a subset or superset of an already found MSS
        if (!isSupersetOfFoundMSS(copy, pFoundMSS)) {
          List<FaultContribution> newTraceAtoms =
              FluentIterable.from(copy)
                  .toSortedList(Comparator.comparingInt(f -> ((TraceAtom) f).getIndex()));
          List<CFAEdge> newTraceAtomsCFA =
              FluentIterable.from(newTraceAtoms).transform(a -> a.correspondingEdge()).toList();
          Trace newTrace =
              Trace.fromCounterexample(
                  newTraceAtomsCFA, pContext, new TraceFormulaOptions(pContext.getConfiguration()));
          BooleanFormula newPostCondition =
              solver
                  .getFormulaManager()
                  .uninstantiate(pTraceFormula.getPostCondition().getPostCondition());
          newPostCondition =
              bmgr.not(
                  solver
                      .getFormulaManager()
                      .instantiate(newPostCondition, newTrace.getLatestSsaMap()));
          BooleanFormula composedFormula =
              bmgr.and(preCondition, bmgr.and(newTrace.toFormulaList()), newPostCondition);
          stats.unsatCalls.inc();

          if (!solver.isUnsat(composedFormula)) {
            changed = true;
            result.add(atom);
            selectors.remove(atom);
            break;
          }
        }
      }
    } while (changed);
    return selectors;
  }

  private boolean isSupersetOfFoundMSS(Fault setToCheck, Set<Fault> foundMSS) {
    for (Fault mss : foundMSS) {
      if (setToCheck.containsAll(mss)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}
