// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
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
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class TraceFormula {

  protected FormulaContext context;
  protected final Selector.Factory selectorFactory;

  protected BooleanFormulaManager bmgr;
  protected PostCondition postcondition;
  protected PreCondition precondition;
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
        description =
            "The alternative precondition consists of all initial variable assignments  and a"
                + " failing variable assignment for all nondet variables. By default only "
                + " variables in the main function are part of the precondition. Overwrite the"
                + " default by adding functions to this option, e.g., \"main,doStuff\"")
    private List<String> filter = ImmutableList.of("main");

    // Usage: If a variable is contained in the post-condition it may be useful to ignore it in the
    // pre-condition
    @Option(
        secure = true,
        name = "ignore",
        description =
            "The alternative precondition consists of all initial variable assignments. If a"
                + " variable assignment seems suspicious, it might be useful to exclude it from the"
                + " precondition. To do this, add these variables to this option, e.g.,"
                + " main::x,doStuff::y. Make sure to add the function in which the variable is used"
                + " as prefix, separated by two ':'")
    private List<String> ignore = ImmutableList.of();

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
        name = "altpre",
        description =
            "By default, the precondition only contains the failing variable assignment of all"
                + " nondet variables. Enable this option if initial variable assignments of the"
                + " form '<datatype> <variable-name> = <value>' should also be added to the"
                + " precondition. See the description for the option traceformula.ignore for"
                + " further options.")
    private boolean forcePre = false;

    @Option(
        secure = true,
        name = "uniqueselectors",
        description =
            "By default, every executed statement gets its own selector. If a loop is part of the"
                + " program to analyze, the number of selectors can increase which also increases"
                + " the run time of max-sat drastically. To use the same selector for equal"
                + " statements (on the same line), set this option to true. Note that enabling this"
                + " option  also decreases the quality of results.")
    private boolean reduceSelectors = false;

    public TraceFormulaOptions(Configuration pConfiguration) throws InvalidConfigurationException {
      pConfiguration.inject(this);
    }

    public List<String> getFilter() {
      return filter;
    }

    public List<String> getDisable() {
      return disable;
    }

    public List<String> getIgnore() {
      return ignore;
    }

    public boolean isReduceSelectors() {
      return reduceSelectors;
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
    selectorFactory = new Selector.Factory();
    edges = pEdges;
    options = pOptions;
    context = pContext;
    bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    calculateEntries();
    precondition = calculatePrecondition();
    postcondition = calculatePostCondition();
    trace = calculateTrace();
    // logs for unit tests
    context.getLogger().log(Level.FINEST, "tftrace=" + trace);
  }

  public boolean isCalculationPossible() throws SolverException, InterruptedException {
    return !context.getSolver().isUnsat(bmgr.and(postcondition.condition, precondition.condition));
  }

  public PostCondition getPostcondition() {
    return postcondition;
  }

  public PreCondition getPrecondition() {
    return precondition;
  }

  public BooleanFormula getTrace() {
    return trace;
  }

  public BooleanFormula getTraceFormula() {
    return bmgr.and(precondition.condition, trace, postcondition.condition);
  }

  public FormulaEntryList getEntries() {
    return entries;
  }

  public FormulaContext getContext() {
    return context;
  }

  public int getPostConditionOffset() {
    return traceSize() - postConditionOffset;
  }

  private PreCondition calculatePrecondition() throws SolverException, InterruptedException {
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(bmgr.and(entries.toAtomList()));
      Preconditions.checkArgument(!prover.isUnsat(), "a model has to be existent");
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        context.getLogger().log(Level.FINEST, "tfprecondition=" + modelAssignment);
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if (!Pattern.matches(".+::.+@[0-9]+", modelAssignment.getKey().toString())) {
          precond = bmgr.and(precond, formula);
        }
      }
    }

    if (options.forcePre && bmgr.isTrue(precond)) {
      return AlternativePrecondition.of(options.filter, options.ignore, precond, context, entries);
    } else {
      entries.addEntry(0, new FormulaEntryList.PreconditionEntry(SSAMap.emptySSAMap()));
    }
    // cannot find edges for model
    return new PreCondition(ImmutableSet.of(), precond);
  }

  public Selector.Factory getSelectorFactory() {
    return selectorFactory;
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
  private PostCondition calculatePostCondition() {
    BooleanFormula postCond = bmgr.makeTrue();
    Set<CFAEdge> containedInPostCondition = new HashSet<>();
    Set<CFAEdge> ignoredEdgesAfterPostCond = new HashSet<>();
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
                      entry -> entry.getAtomId() == currI, FormulaEntryList.FormulaEntry::getAtom));
          postCond = bmgr.and(postCond, formula);
          context
              .getLogger()
              .log(
                  Level.FINEST,
                  "tfpostcondition=" + curr.getFileLocation().getStartingLineInOrigin());
          containedInPostCondition.add(curr);
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
                        entry -> {
                          if (entry instanceof FormulaEntryList.PreconditionEntry
                              || entry.getSelector() == null) {
                            return false;
                          }
                          return entry.getSelector().correspondingEdge().equals(curr);
                        },
                        FormulaEntryList.FormulaEntry::getAtom));
            context
                .getLogger()
                .log(
                    Level.FINEST,
                    "tfpostcondition=line " + curr.getFileLocation().getStartingLineInOrigin());
            postCond = bmgr.and(postCond, formula);
            containedInPostCondition.add(curr);
            postConditionOffset = i;
          }
        }
      } else {
        // ensures that only consecutive assume edges are valid
        if (lastAssume != -1) {
          break;
        }
        ignoredEdgesAfterPostCond.add(curr);
      }
    }
    return new PostCondition(
        containedInPostCondition, ignoredEdgesAfterPostCond, bmgr.not(postCond));
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
          ImmutableList.copyOf(bmgr.toConjunctionArgs(current.getFormula(), false));
      BooleanFormula currentAtom = formulaList.get(0);
      if (formulaList.size() == 2) {
        if (i == 0) {
          currentAtom = bmgr.and(currentAtom, formulaList.get(1));
        } else if (formulaList.get(0).equals(prev)) {
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
        selector = selectorFactory.makeSelector(context, currentAtom, e);
        foundSelectors.put(selectorIdentifier, selector);
      }
      entries.addEntry(i, current.getSsa(), selector, currentAtom);
    }

    // disable selectors
    if (!options.disable.isEmpty()) {
      for (int i = 0; i < entries.size(); i++) {
        String formulaString = entries.toAtomList().get(i).toString();
        Selector selector = entries.toSelectorList().get(i);
        for (String disable : options.disable) {
          if (formulaString.contains(disable)) {
            selector.disable();
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
    return bmgr.and(entries.toAtomList().subList(start, end));
  }

  public int traceSize() {
    return entries.toAtomList().size();
  }

  @Override
  public String toString() {
    return "TraceFormula{" + getTraceFormula() + "}";
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
                break;
              }
            case ENDIF:
              {
                // an if statement ended here -> pop it from the stack
                conditions.pop();
                break;
              }
            default:
              throw new AssertionError("Not a valid label: " + label);
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

  public interface TraceFormulaConstraint {
    BooleanFormula condition();

    ImmutableSet<CFAEdge> responsibleEdges();
  }

  public static class PreCondition implements TraceFormulaConstraint {

    private final BooleanFormula condition;
    private final ImmutableSet<CFAEdge> responsibleEdges;

    public PreCondition(Set<CFAEdge> pEdgesInPrecondition, BooleanFormula pFormula) {
      responsibleEdges = ImmutableSet.copyOf(pEdgesInPrecondition);
      condition = pFormula;
    }

    @Override
    public BooleanFormula condition() {
      return condition;
    }

    @Override
    public ImmutableSet<CFAEdge> responsibleEdges() {
      return responsibleEdges;
    }
  }

  public static class PostCondition implements TraceFormulaConstraint {

    private final BooleanFormula condition;
    private final ImmutableSet<CFAEdge> responsibleEdges;
    private final ImmutableSet<CFAEdge> ignoredEdges;

    public PostCondition(
        Set<CFAEdge> pEdgesInPrecondition, Set<CFAEdge> pIgnoredEdges, BooleanFormula pFormula) {
      responsibleEdges = ImmutableSet.copyOf(pEdgesInPrecondition);
      ignoredEdges = ImmutableSet.copyOf(pIgnoredEdges);
      condition = pFormula;
    }

    @Override
    public BooleanFormula condition() {
      return condition;
    }

    @Override
    public ImmutableSet<CFAEdge> responsibleEdges() {
      return responsibleEdges;
    }

    public ImmutableSet<CFAEdge> getIgnoredEdges() {
      return ignoredEdges;
    }
  }
}
