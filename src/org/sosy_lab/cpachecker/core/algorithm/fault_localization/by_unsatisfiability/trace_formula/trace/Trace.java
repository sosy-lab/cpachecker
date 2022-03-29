// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.AbstractTraceElement;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace.TraceAtom;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.dependencegraph.MergePoint;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;

public class Trace extends ForwardingList<TraceAtom> {

  private enum FormulaLabel {
    IF,
    ENDIF
  }

  private final ImmutableList<TraceAtom> entries;
  private final SSAMap latestSSAMap;
  private final SSAMap initialSSAMap;
  private final FormulaContext context;

  private Trace(FormulaContext pContext, List<TraceAtom> pEntries) {
    entries = ImmutableList.copyOf(pEntries);
    context = pContext;
    if (entries.isEmpty()) {
      latestSSAMap = SSAMap.emptySSAMap();
      initialSSAMap = SSAMap.emptySSAMap();
    } else {
      latestSSAMap = entries.get(entries.size() - 1).ssaMap;
      initialSSAMap = calculateInitialSsaMap();
    }
  }

  public SSAMap getLatestSsaMap() {
    return latestSSAMap;
  }

  public SSAMap getInitialSsaMap() {
    return initialSSAMap;
  }

  private SSAMap calculateInitialSsaMap() {
    Map<String, Integer> minIndexMap = new HashMap<>();
    Map<String, CType> typeMap = new HashMap<>();
    for (TraceAtom traceAtom : this) {
      context
          .getSolver()
          .getFormulaManager()
          .extractVariables(traceAtom.formula)
          .forEach(
              (name, variable) -> {
                Pair<String, OptionalInt> pair = FormulaManagerView.parseName(name);
                if (Objects.requireNonNull(pair.getSecond()).isPresent()) {
                  minIndexMap.merge(pair.getFirst(), pair.getSecond().orElseThrow(), Integer::min);
                  typeMap.put(
                      pair.getFirst(),
                      traceAtom.ssaMap.getType(Objects.requireNonNull(pair.getFirst())));
                }
              });
    }
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (Entry<String, Integer> variableIndexEntry : minIndexMap.entrySet()) {
      builder =
          builder.setIndex(
              variableIndexEntry.getKey(),
              typeMap.get(variableIndexEntry.getKey()),
              variableIndexEntry.getValue());
    }
    return builder.build();
  }

  /**
   * Transforms the current trace to a flow-sensitive trace. The original trace will not be
   * modified. Flow-sensitive traces do not contain assume edges. Instead, assume edges imply
   * successive edges as long as the edges are only reachable through the assume edge.
   *
   * @return flow-sensitive trace
   */
  private Trace makeFlowSensitive() {
    if (entries.isEmpty()) {
      return this;
    }
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    Map<CFANode, Integer> mergeNodes = new HashMap<>();
    MergePoint<CFANode> mergePoint =
        new MergePoint<>(
            entries.get(0).correspondingEdge().getPredecessor(),
            CFAUtils::allSuccessorsOf,
            CFAUtils::allPredecessorsOf);
    List<List<FormulaLabel>> labels = new ArrayList<>(entries.size());
    for (TraceAtom entry : entries) {
      List<FormulaLabel> labelsForEntry = new ArrayList<>();
      if (entry.correspondingEdge().getEdgeType() == CFAEdgeType.AssumeEdge) {
        labelsForEntry.add(FormulaLabel.IF);
        mergeNodes.merge(
            mergePoint.findMergePoint(entry.correspondingEdge().getPredecessor()), 1, Integer::sum);
      }
      for (int i = 0;
          i < mergeNodes.getOrDefault(entry.correspondingEdge().getSuccessor(), 0);
          i++) {
        labelsForEntry.add(0, FormulaLabel.ENDIF);
      }
      labels.add(labelsForEntry);
    }

    ArrayDeque<BooleanFormula> conditions = new ArrayDeque<>();
    List<TraceAtom> flowSensitiveList = new ArrayList<>(entries.size());
    int index = 0;
    for (int i = 0; i < labels.size(); i++) {
      List<FormulaLabel> currentLabels = labels.get(i);
      TraceAtom currentTraceAtom = entries.get(i);
      boolean isIf = false;
      for (FormulaLabel currentLabel : currentLabels) {
        if (currentLabel == FormulaLabel.ENDIF) {
          conditions.pop();
        } else if (currentLabel == FormulaLabel.IF) {
          conditions.push(currentTraceAtom.formula);
          isIf = true;
        }
      }
      if (!isIf) {
        if (conditions.isEmpty()) {
          flowSensitiveList.add(
              new TraceAtom(
                  index,
                  currentTraceAtom.selector,
                  currentTraceAtom.formula,
                  currentTraceAtom.ssaMap,
                  currentTraceAtom.correspondingEdge()));
        } else {
          flowSensitiveList.add(
              new TraceAtom(
                  index,
                  currentTraceAtom.selector,
                  bmgr.implication(bmgr.and(conditions), currentTraceAtom.formula),
                  currentTraceAtom.ssaMap,
                  currentTraceAtom.correspondingEdge()));
        }
        index++;
      }
    }
    return new Trace(context, flowSensitiveList);
  }

  /**
   * Creates a {@link Trace} from a counterexample represented as list (path) of {@link CFAEdge}s
   *
   * @param pCounterexample path to an error location
   * @param pContext context of formula containing managers and solver to create formulas
   * @param pOptions user-specified options for the trace formula
   * @return a list of {@link TraceAtom}s representing a trace based on a given counterexample
   * @throws CPATransferException thrown if {@link PathFormulaManagerImpl#makeAnd(PathFormula,
   *     CFAEdge) fails.}
   * @throws InterruptedException thrown if {@link PathFormulaManagerImpl#makeAnd(PathFormula,
   *     CFAEdge) fails.}
   */
  public static Trace fromCounterexample(
      List<CFAEdge> pCounterexample, FormulaContext pContext, TraceFormulaOptions pOptions)
      throws CPATransferException, InterruptedException {
    List<TraceAtom> atoms = new ArrayList<>(pCounterexample.size());
    FormulaManagerView fmgr = pContext.getSolver().getFormulaManager();
    BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
    PathFormulaManagerImpl manager = pContext.getManager();
    Map<CFAEdge, BooleanFormula> edgeToSelector = new HashMap<>();

    PathFormula previousPathFormula = null;
    int index = 0;
    for (CFAEdge cfaEdge : pCounterexample) {
      // do not create selectors for edges with path formula true as they will never contribute or
      // cause a violation
      if (bmgr.isTrue(manager.makeAnd(manager.makeEmptyPathFormula(), cfaEdge).getFormula())) {
        continue;
      }
      final BooleanFormula selector;
      if (pOptions.isReduceSelectors()) {
        selector = edgeToSelector.getOrDefault(cfaEdge, makeSelectorFormula(fmgr, index));
        edgeToSelector.put(cfaEdge, selector);
      } else {
        selector = makeSelectorFormula(fmgr, index);
      }
      if (previousPathFormula == null) {
        previousPathFormula = manager.makeAnd(manager.makeEmptyPathFormula(), cfaEdge);
        atoms.add(
            new TraceAtom(
                index,
                selector,
                previousPathFormula.getFormula(),
                previousPathFormula.getSsa(),
                cfaEdge));
      } else {
        PathFormula currentPathFormula = manager.makeAnd(previousPathFormula, cfaEdge);
        Set<BooleanFormula> parts =
            new HashSet<>(bmgr.toConjunctionArgs(currentPathFormula.getFormula(), false));
        parts.remove(previousPathFormula.getFormula());
        BooleanFormula currentBooleanFormula = Iterables.getOnlyElement(parts);
        atoms.add(
            new TraceAtom(
                index, selector, currentBooleanFormula, currentPathFormula.getSsa(), cfaEdge));
        previousPathFormula = currentPathFormula;
      }
      index++;
    }
    if (pOptions.makeFlowSensitive()) {
      return new Trace(pContext, atoms).makeFlowSensitive();
    }
    return new Trace(pContext, atoms);
  }

  private static BooleanFormula makeSelectorFormula(FormulaManagerView fmgr, int index) {
    return fmgr.makeVariable(FormulaType.BooleanType, "S." + index);
  }

  public List<BooleanFormula> toSelectorList() {
    return Lists.transform(this, atom -> atom.selector);
  }

  public List<BooleanFormula> toFormulaList() {
    return Lists.transform(this, atom -> atom.formula);
  }

  public List<SSAMap> toSSAMapList() {
    return Lists.transform(this, atom -> atom.ssaMap);
  }

  public List<CFAEdge> toEdgeList() {
    return Lists.transform(this, atom -> atom.correspondingEdge());
  }

  /**
   * Get all elements up to and including {@code end - 1}
   *
   * @param end cut the trace at position {@code end}
   * @return all trace elements up to {@code end} as {@link Trace}
   */
  public Trace slice(int end) {
    return slice(0, end);
  }

  /**
   * Get all elements from start up to and including {@code end - 1}
   *
   * @param start start at position {@code start}
   * @param end cut the trace at position {@code end}
   * @return all trace elements from {@code start} up to {@code end} as {@link Trace}
   */
  public Trace slice(int start, int end) {
    return new Trace(context, subList(start, end));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("size", size()).toString();
  }

  @Override
  protected List<TraceAtom> delegate() {
    return entries;
  }

  /** TraceAtoms are the entries of the Trace. */
  public static class TraceAtom extends FaultContribution implements AbstractTraceElement {

    private final BooleanFormula selector;
    private final BooleanFormula formula;
    private final SSAMap ssaMap;
    private final int index;

    private TraceAtom(
        int pIndex,
        BooleanFormula pSelector,
        BooleanFormula pFormula,
        SSAMap pSSAMap,
        CFAEdge pEdge) {
      super(pEdge);
      index = pIndex;
      selector = pSelector;
      formula = pFormula;
      ssaMap = pSSAMap;
    }

    public BooleanFormula getFormula() {
      return formula;
    }

    public BooleanFormula getSelector() {
      return selector;
    }

    public int getIndex() {
      return index;
    }

    public SSAMap getSsaMap() {
      return ssaMap;
    }

    @Override
    public boolean equals(Object pO) {
      if (!(pO instanceof TraceAtom)) {
        return false;
      }
      TraceAtom traceAtom = (TraceAtom) pO;
      return index == traceAtom.index && super.equals(pO);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), index);
    }
  }
}
