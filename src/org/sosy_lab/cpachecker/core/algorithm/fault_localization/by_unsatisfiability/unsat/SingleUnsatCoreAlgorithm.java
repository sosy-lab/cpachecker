// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat;

import com.google.common.base.VerifyException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.trace.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.trace.Selector;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.formula.trace.TraceFormula;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class SingleUnsatCoreAlgorithm implements FaultLocalizerWithTraceFormula, Statistics {

  private StatTimer totalTime = new StatTimer("Total time");

  /**
   * Calculate a single unsat-core (not necessarily minimal) and return the set.
   *
   * @param context the formula context
   * @param tf the traceformula for the program
   * @return one UNSAT-core
   */
  @Override
  public Set<Fault> run(FormulaContext context, TraceFormula tf)
      throws CPATransferException, InterruptedException, SolverException, VerifyException {

    Solver solver = context.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    totalTime.start();
    // if precondition is not needed to guarantee unsatisfiability, do not add it and obtain better
    // results (i.e. find locations independently from the inputs first).
    BooleanFormula toVerify = bmgr.and(tf.getTrace(), tf.getPostcondition());
    if (!solver.isUnsat(toVerify)) {
      toVerify = bmgr.and(tf.getPrecondition(), toVerify);
    }

    // calculate an arbitrary UNSAT-core and filter the ones with selectors
    List<Selector> unsatCore =
        solver.unsatCore(toVerify).stream()
            .filter(l -> Selector.of(l).isPresent())
            .map(l -> Selector.of(l).orElseThrow())
            .collect(Collectors.toList());

    totalTime.stop();
    Set<Fault> resultSet = new HashSet<>();
    resultSet.add(new Fault(new HashSet<>(unsatCore)));
    return resultSet;
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
    w0.put("Total time", totalTime);
  }

  @Override
  public @Nullable String getName() {
    return "Single UNSAT-core";
  }
}
