// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.PostCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.PostConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.PreCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.PreConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.TraceInterpreter;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class TraceFormula {

  @Options(prefix = "traceformula")
  public static class TraceFormulaOptions {

    @Option(
        secure = true,
        name = "ignore",
        description =
            "The alternative precondition consists of all initial variable assignments. If a"
                + " variable assignment seems suspicious, it might be useful to exclude it from the"
                + " precondition. To do this, add these variables to this option, e.g.,"
                + " main::x,doStuff::y. Make sure to add the function in which the variable is used"
                + " as prefix, separated by two ':'")
    private List<String> excludeFromPrecondition = ImmutableList.of();

    // Usage: If a variable is contained in the post-condition it may be useful to ignore it in the
    // pre-condition
    @Option(
        secure = true,
        name = "filter",
        description =
            "The alternative precondition consists of all initial variable assignments and a"
                + " failing variable assignment for all nondet variables. By default only "
                + " variables in the main function are part of the precondition. Overwrite the"
                + " default by adding functions to this option, e.g., \"main,doStuff\"")
    private List<String> functionsForPrecondition = ImmutableList.of("main");

    @Option(
        secure = true,
        name = "disable",
        description =
            "Usually every statement that is not part of the precondition gets a selector. If a"
                + " certain variable is known to not cause the error, add it to this option, e.g., "
                + "main::x,doStuff::y")
    private List<String> disable = ImmutableList.of();

    @Option(
        secure = true,
        description =
            "By default, every executed statement gets its own selector. If a loop is part of the"
                + " program to analyze, the number of selectors can increase which also increases"
                + " the run time of max-sat drastically. To use the same selector for equal"
                + " statements (on the same line), set this option to true. Note that enabling this"
                + " option  also decreases the quality of results.")
    private boolean reduceSelectors = false;

    @Option(
        secure = true,
        description =
            "Make trace formula flow-sensitive, i.e., assume edges imply the edges that are only"
                + " reachable through the assume edge. Flow-sensitive traces remove assume edges"
                + " from the trace. Hence, no assume edge will be part of a fault.")
    private boolean makeFlowSensitive = false;

    public TraceFormulaOptions(Configuration pConfiguration) throws InvalidConfigurationException {
      pConfiguration.inject(this);
    }

    public List<String> getExcludeFromPrecondition() {
      return excludeFromPrecondition;
    }

    public List<String> getDisable() {
      return disable;
    }

    public List<String> getFunctionsForPrecondition() {
      return functionsForPrecondition;
    }

    public boolean isReduceSelectors() {
      return reduceSelectors;
    }

    public boolean makeFlowSensitive() {
      return makeFlowSensitive;
    }
  }

  private final FormulaContext context;
  private final PreCondition precondition;
  private final Trace trace;
  private final PostCondition postCondition;

  private TraceFormula(
      FormulaContext pContext,
      PreCondition pPreCondition,
      Trace pTrace,
      PostCondition pPostCondition) {
    precondition = pPreCondition;
    trace = pTrace;
    postCondition = pPostCondition;
    context = pContext;
  }

  public BooleanFormula toFormula(
      TraceInterpreter pTraceInterpreter, boolean pNegatePostCondition) {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula postConditionFormula =
        pNegatePostCondition
            ? bmgr.not(postCondition.getPostCondition())
            : postCondition.getPostCondition();
    return bmgr.and(
        precondition.getPrecondition(), pTraceInterpreter.interpret(trace), postConditionFormula);
  }

  /**
   * Check whether the instantiated precondition contradicts the instantiated post-condition. In
   * case it does, no further analysis is required since the program does not change the variables
   * in the precondition. Hence, no error-prone statements will be found as they do not cause the
   * contradiction.
   *
   * @return whether fault localization can succeed
   * @throws SolverException if solver encounters a problem
   * @throws InterruptedException if program is interrupted unexpectedly
   */
  public boolean isCalculationPossible() throws SolverException, InterruptedException {
    Solver solver = context.getSolver();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    return !solver.isUnsat(
        bmgr.and(precondition.getPrecondition(), bmgr.not(postCondition.getPostCondition())));
  }

  public PostCondition getPostCondition() {
    return postCondition;
  }

  public PreCondition getPrecondition() {
    return precondition;
  }

  public Trace getTrace() {
    return trace;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("size", trace.size()).toString();
  }

  public static TraceFormula instantiate(
      FormulaContext pContext,
      PreCondition pPrecondition,
      Trace pTrace,
      PostCondition pPostCondition) {
    FormulaManagerView fmgr = pContext.getSolver().getFormulaManager();
    return new TraceFormula(
        pContext,
        pPrecondition.instantiate(fmgr, pTrace.getInitialSsaMap()),
        pTrace,
        pPostCondition.instantiate(fmgr, pTrace.getLatestSsaMap()));
  }

  public static TraceFormula fromCounterexample(
      PreConditionComposer pPreConditionType,
      PostConditionComposer pPostConditionType,
      List<CFAEdge> pCounterexample,
      FormulaContext pContext,
      TraceFormulaOptions pOptions)
      throws CPATransferException, SolverException, InterruptedException {
    List<CFAEdge> remainingCounterexample = pCounterexample;
    PreCondition precondition = pPreConditionType.extractPreCondition(remainingCounterexample);
    remainingCounterexample = precondition.getRemainingCounterexample();
    PostCondition postCondition = pPostConditionType.extractPostCondition(remainingCounterexample);
    remainingCounterexample = postCondition.getRemainingCounterexample();
    Trace trace = Trace.fromCounterexample(remainingCounterexample, pContext, pOptions);
    return instantiate(pContext, precondition, trace, postCondition);
  }
}
