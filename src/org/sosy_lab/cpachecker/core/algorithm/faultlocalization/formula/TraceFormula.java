// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class TraceFormula {

  public enum TraceFormulaType {
    // conjunct of pre trace and post
    DEFAULT,
    // selectors for every atom in trace
    SELECTOR,
    // fstf (implies if blocks)
    FLOW_SENSITIVE
  }

  private FormulaContext context;

  private BooleanFormulaManager bmgr;
  private BooleanFormula postcondition;
  private BooleanFormula precondition;
  private BooleanFormula trace;

  private int postConditionOffset;

  private FormulaEntryList entries;
  private List<CFAEdge> edges;

  protected TraceFormulaOptions options;

  @Options(prefix="traceformula")
  public static class TraceFormulaOptions {
    @Option(secure=true, name="filter",
        description="filter the alternative precondition by scopes")
    private String filter = "main";

    //Usage: If a variable is contained in the post-condition it may be useful to ignore it in the pre-condition
    @Option(secure=true, name="ignore",
        description="do not add variables to alternative precondition (separate by commas)")
    private String ignore = "";

    //Usage: If a variable is contained in the post-condition it may be useful to ignore it in the pre-condition
    @Option(secure=true, name="disable",
        description="do not create selectors for this variables (separate by commas)")
    private String disable = "";

    @Option(
        secure = true,
        name = "altpre",
        description = "force alternative pre condition")
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
   * Additionally creates a trace formula with selectors.
   * @param pContext commonly used objects
   * @param pOptions set options for trace formula
   * @param pEdges counterexample
   */
  public TraceFormula(TraceFormulaType pType, FormulaContext pContext, TraceFormulaOptions pOptions, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException, SolverException {
    entries = new FormulaEntryList();
    edges = pEdges;
    options = pOptions;
    context = pContext;
    bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    calculateEntries();
    postcondition = calculatePostCondition();
    precondition = calculatePrecondition();
    Preconditions.checkArgument(!context.getSolver().isUnsat(bmgr.and(postcondition, precondition)),
        "Pre- and post-condition are unsatisfiable. Further analysis is not possible.");
    trace = calculateTrace(pType);
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
    return postConditionOffset;
  }

  private BooleanFormula calculatePrecondition() throws SolverException, InterruptedException {
    BooleanFormula precond = bmgr.makeTrue();
    try (ProverEnvironment prover = context.getProver()) {
      prover.push(bmgr.and(entries.toAtomList()));
      Preconditions.checkArgument(!prover.isUnsat(), "a model has to be existent");
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if(formula.toString().contains("__VERIFIER_nondet")){
          precond = bmgr.and(precond, formula);
        }
      }
    }

    if (options.forcePre && bmgr.isTrue(precond)) {
      return new AlternativePrecondition(options.filter, options.ignore, precond).createFormula(context, edges, entries);
    }
    entries.addEntry(0,-1, SSAMap.emptySSAMap(), null, null);
    return precond;
  }

  /**
   * Calculate trace
   */
  private BooleanFormula calculateTrace(TraceFormulaType type) {
    switch(type) {
      case SELECTOR: return entries
          .toSelectorList()
          .stream()
          .map(entry -> bmgr.implication(entry.getFormula(), entry.getEdgeFormula()))
          .collect(bmgr.toConjunction());
      case FLOW_SENSITIVE: //fall through //TODO
      case DEFAULT: //fall through
      default: return bmgr.and(entries.toAtomList());
    }
  }

  private BooleanFormula calculatePostCondition() {
    BooleanFormula postCond = bmgr.makeTrue();
    int lastAssume = -1;
    for (int i = edges.size() - 1; i >= 0; i--) {
      CFAEdge curr = edges.get(i);
      if (curr.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        if (lastAssume == -1) {
          //init position and postcond
          final int currI = i;
          lastAssume = curr.getFileLocation().getStartingLineInOrigin();
          BooleanFormula formula = bmgr.and(entries.removeExtract(entry -> entry.getAtomId()==currI, edge -> edge.getAtom()));
          postCond = bmgr.and(postCond, formula);
          postConditionOffset = i;
        } else {
          // as soon as curr is on another line or the edge type changes, break. Otherwise add to postcond.
          if (lastAssume != curr.getFileLocation().getStartingLineInOrigin()) {
            break;
          } else {
            BooleanFormula formula = bmgr.and(entries.removeExtract(entry -> entry.getSelector().getEdge().equals(curr), edge -> edge.getAtom()));
            postCond = bmgr.and(postCond, formula);
            postConditionOffset = i;
          }
        }
      } else {
        //ensures that only consecutive assume edges are valid
        if (lastAssume != -1) {
          break;
        }
      }
    }
    return bmgr.not(postCond);
  }

  /**
   * Calculate the boolean formulas for every edge including the SSA-maps and the selectors.
   * @param altPre Creates the alternative precondition on the fly regardless of set options
   */
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
      String selectorIdentifier = e.getDescription() + " "
          + e.getEdgeType() + " "
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

  public BooleanFormula slice(int end) {
    return slice(0, end);
  }

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
}
