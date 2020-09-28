// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.LabeledCounterexample.FormulaLabel;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.LabeledCounterexample.LabeledFormula;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class TraceFormula {

  protected FormulaContext context;

  protected BooleanFormulaManager bmgr;
  protected BooleanFormula postcondition;
  protected BooleanFormula precondition;
  protected BooleanFormula trace;

  protected int postConditionOffset;

  protected FormulaEntryList entries;
  protected List<CFAEdge> edges;

  protected TraceFormulaOptions options;

  @Options(prefix = "traceformula")
  public static class TraceFormulaOptions {
    @Option(
        secure = true,
        name = "filter",
        description = "filter the alternative precondition by scopes")
    private String filter = "main";

    // Usage: If a variable is contained in the post-condition it may be useful to ignore it in the
    // pre-condition
    @Option(
        secure = true,
        name = "ignore",
        description = "do not add variables to alternative precondition (separate by commas)")
    private String ignore = "";

    // Usage: If a variable is contained in the post-condition it may be useful to ignore it in the
    // pre-condition
    @Option(
        secure = true,
        name = "disable",
        description = "do not create selectors for this variables (separate by commas)")
    private String disable = "";

    @Option(
        secure = true,
        name = "altpre",
        description =
            "add initial variable assignments to the pre-condition instead of just using failing"
                + " variable assignments for nondet variables")
    private boolean forcePre = false;

    @Option(
        secure = true,
        name = "uniqueselectors",
        description = "equal statements on the same line get the same selector")
    private boolean reduceSelectors = false;

    public TraceFormulaOptions(Configuration pConfiguration) throws InvalidConfigurationException {
      pConfiguration.inject(this);
    }

    public String getFilter() {
      return filter;
    }

    public String getDisable() {
      return disable;
    }

    public String getIgnore() {
      return ignore;
    }

    public boolean isReduceSelectors() {
      return reduceSelectors;
    }

    public void setReduceSelectors(boolean pReduceSelectors) {
      reduceSelectors = pReduceSelectors;
    }
  }

  /**
   * Creates the trace formula for a given list of CFAEdges.
   *
   * @param pContext commonly used objects
   * @param pOptions set options for trace formula
   * @param pEdges counterexample
   */
  private TraceFormula(FormulaContext pContext, TraceFormulaOptions pOptions, List<CFAEdge> pEdges)
      throws CPAException, InterruptedException, SolverException {
    entries = new FormulaEntryList();
    edges = pEdges;
    options = pOptions;
    context = pContext;
    bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    calculateEntries();
    postcondition = calculatePostCondition();
    precondition = calculatePrecondition();
    trace = calculateTrace();
  }

  public boolean isCalculationPossible() throws SolverException, InterruptedException {
    return !context.getSolver().isUnsat(bmgr.and(postcondition, precondition));
  }

  public BooleanFormula getPostcondition() {
    return postcondition;
  }

  public BooleanFormula getPrecondition() {
    return precondition;
  }

  public BooleanFormula getTrace() {
    return trace;
  }

  public BooleanFormula getTraceFormula() {
    return bmgr.and(precondition, trace, postcondition);
  }

  public FormulaEntryList getEntries() {
    return entries;
  }

  public int getPostConditionOffset() {
    return traceSize() - postConditionOffset;
  }

  private BooleanFormula calculatePrecondition() throws SolverException, InterruptedException {
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(bmgr.and(entries.toAtomList()));
      Preconditions.checkArgument(!prover.isUnsat(), "a model has to be existent");
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if (formula.toString().contains("__VERIFIER_nondet")) {
          precond = bmgr.and(precond, formula);
        }
      }
    }

    if (options.forcePre && bmgr.isTrue(precond)) {
      return AlternativePrecondition.of(options.filter, options.ignore, precond, context, entries);
    } else {
      entries.addEntry(0, -1, SSAMap.emptySSAMap(), null, null);
    }
    return precond;
  }

  /**
   * Calculate trace
   *
   * @return the trace pi according to the inputted type
   */
  protected abstract BooleanFormula calculateTrace();

  /**
   * Calculates the post-condition as the conjunct of the last consecutive assume edges
   *
   * @return post-condition
   */
  private BooleanFormula calculatePostCondition() {
    BooleanFormula postCond = bmgr.makeTrue();
    int lastAssume = -1;
    for (int i = edges.size() - 1; i >= 0; i--) {
      CFAEdge curr = edges.get(i);
      if (curr.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        if (lastAssume == -1) {
          // init position and postcond
          final int currI = i;
          lastAssume = curr.getFileLocation().getStartingLineInOrigin();
          BooleanFormula formula =
              bmgr.and(
                  entries.removeExtract(
                      entry -> entry.getAtomId() == currI, edge -> edge.getAtom()));
          postCond = bmgr.and(postCond, formula);
          postConditionOffset = i;
        } else {
          // as soon as curr is on another line or the edge type changes, break. Otherwise add to
          // postcond.
          if (lastAssume != curr.getFileLocation().getStartingLineInOrigin()) {
            break;
          } else {
            BooleanFormula formula =
                bmgr.and(
                    entries.removeExtract(
                        entry -> entry.getSelector().getEdge().equals(curr),
                        edge -> edge.getAtom()));
            postCond = bmgr.and(postCond, formula);
            postConditionOffset = i;
          }
        }
      } else {
        // ensures that only consecutive assume edges are valid
        if (lastAssume != -1) {
          break;
        }
      }
    }
    return bmgr.not(postCond);
  }

  /** Calculate the boolean formulas for every edge including the SSA-maps and the selectors. */
  private void calculateEntries() throws CPATransferException, InterruptedException {
    PathFormulaManagerImpl manager = context.getManager();

    // add the current edge formula to the previous one to update the ssa-map, then split them again
    // and add the new part as atom to the entry list.
    // all edges that are at the same line as the last assume edge are added to "negate"
    PathFormula current = manager.makeEmptyPathFormula();
    Map<String, Selector> foundSelectors = new HashMap<>();
    for (int i = 0; i < edges.size(); i++) {
      CFAEdge e = edges.get(i);
      BooleanFormula prev = current.getFormula();
      current = manager.makeAnd(current, e);
      List<BooleanFormula> formulaList =
          new ArrayList<>(bmgr.toConjunctionArgs(current.getFormula(), false));
      BooleanFormula currentAtom = formulaList.get(0);
      if (formulaList.size() == 2) {
        if (formulaList.get(0).equals(prev)) {
          currentAtom = formulaList.get(1);
        }
      }
      String selectorIdentifier =
          e.getDescription()
              + " "
              + e.getEdgeType()
              + " "
              + e.getFileLocation().getStartingLineInOrigin();
      Selector selector;
      if (options.reduceSelectors && foundSelectors.containsKey(selectorIdentifier)) {
        selector = foundSelectors.get(selectorIdentifier);
      } else {
        selector = Selector.makeSelector(context, currentAtom, e);
        foundSelectors.put(selectorIdentifier, selector);
      }
      entries.addEntry(i, current.getSsa(), selector, currentAtom);
    }

    // disable selectors
    if (!options.disable.isEmpty()) {
      List<String> disabled = Splitter.on(",").splitToList(options.disable);
      for (int i = 0; i < entries.size(); i++) {
        String formulaString = entries.toAtomList().get(i).toString();
        Selector selector = entries.toSelectorList().get(i);
        for (String dable : disabled) {
          if (dable.contains("::")) {
            if (formulaString.contains(dable)) {
              selector.disable();
            }
          } else {
            if (formulaString.contains("::" + dable)) {
              selector.disable();
            }
          }
        }
      }
    }
  }

  /**
   * Get all elements from 0 up to and including end-1
   *
   * @param end cut the trace at position end
   * @return all elements from the trace up to position end as boolean formula
   */
  public BooleanFormula slice(int end) {
    return slice(0, end);
  }

  /**
   * Get all elements from start up to and including end-1
   *
   * @param start start at position <code>start</code>
   * @param end cut the trace at position <code>end</code>>
   * @return all trace elements from start up to position end as boolean formula
   */
  public BooleanFormula slice(int start, int end) {
    List<BooleanFormula> atoms = entries.toAtomList();
    BooleanFormula slice = bmgr.makeTrue();
    for (int i = start; i < end; i++) {
      slice = bmgr.and(atoms.get(i), slice);
    }
    return slice;
  }

  public int traceSize() {
    return entries.toAtomList().size();
  }

  @Override
  public String toString() {
    return "TraceFormula{" + BooleanFormulaParser.parse(getTraceFormula()) + "}";
  }

  public static class SelectorTrace extends TraceFormula {

    public SelectorTrace(
        FormulaContext pFormulaContext,
        TraceFormulaOptions pTraceFormulaOptions,
        List<CFAEdge> pCounterexample)
        throws CPAException, InterruptedException, SolverException {
      super(pFormulaContext, pTraceFormulaOptions, pCounterexample);
    }

    @Override
    protected BooleanFormula calculateTrace() {
      return entries.toSelectorList().stream()
          .map(entry -> bmgr.implication(entry.getFormula(), entry.getEdgeFormula()))
          .collect(bmgr.toConjunction());
    }
  }

  public static class DefaultTrace extends TraceFormula {

    public DefaultTrace(
        FormulaContext pFormulaContext,
        TraceFormulaOptions pTraceFormulaOptions,
        List<CFAEdge> pCounterexample)
        throws CPAException, InterruptedException, SolverException {
      super(pFormulaContext, pTraceFormulaOptions, pCounterexample);
    }

    @Override
    protected BooleanFormula calculateTrace() {
      return bmgr.and(entries.toAtomList());
    }
  }

  public static class FlowSensitiveTrace extends TraceFormula {

    public FlowSensitiveTrace(
        FormulaContext pFormulaContext,
        TraceFormulaOptions pTraceFormulaOptions,
        List<CFAEdge> pCounterexample)
        throws CPAException, InterruptedException, SolverException {
      super(pFormulaContext, pTraceFormulaOptions, pCounterexample);
    }

    @Override
    protected BooleanFormula calculateTrace() {
      makeFlowSensitive();
      return bmgr.and(entries.toAtomList());
    }

    /** Modify statements such that all dominating assumes imply the statement. Cannot be undone. */
    private void makeFlowSensitive() {
      // NOTE: can be undone by manually coping the current "entries" and replacing it afterwards.
      // NOTE: Edges containing the label ENDIF indicate that their predecessor nodes are merge
      // points.
      LabeledCounterexample cex = new LabeledCounterexample(entries, context);
      ArrayDeque<BooleanFormula> conditions = new ArrayDeque<>();

      boolean isIf;
      for (LabeledFormula edge : cex) {

        isIf = false;
        for (FormulaLabel label : edge.getLabels()) {

          switch (label) {
            case IF:
              {
                // add a condition to the stack
                conditions.push(edge.getEntry().getAtom());
                entries.remove(edge.getEntry());
                isIf = true;
                continue;
              }
            case ENDIF:
              {
                // an if statement ended here -> pop it from the stack
                conditions.pop();
                continue;
              }
            default:
              continue;
          }
        }

        // if the current edge is not an assume edge replace the atom with the implication
        if (!isIf) {
          BooleanFormula conditionsConjunct = bmgr.and(conditions);
          BooleanFormula implication =
              bmgr.implication(conditionsConjunct, edge.getEntry().getAtom());
          edge.getEntry().setAtom(implication);
        }
      }
    }
  }
}
