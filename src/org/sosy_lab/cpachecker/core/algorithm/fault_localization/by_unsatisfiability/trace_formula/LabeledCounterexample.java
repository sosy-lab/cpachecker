// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.collect.ForwardingList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.LabeledCounterexample.LabeledFormula;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.MergePoint;

final class LabeledCounterexample extends ForwardingList<LabeledFormula> {

  enum FormulaLabel {
    // assume edges are IF statements
    IF,
    // edges that are the last statement of an if-block are ENDIF statements
    ENDIF
  }

  private List<LabeledFormula> annotatedCounterexample;

  /**
   * The LabeledCounterexample adds the labels IF and ENDIF to fitting statements in the
   * counterexample
   *
   * @param pCounterexample the counterexample for which we want to compute the labels
   * @param pContext the context
   */
  public LabeledCounterexample(FormulaEntryList pCounterexample, FormulaContext pContext) {
    annotatedCounterexample = new ArrayList<>();
    CFA cfa = pContext.getMutableCFA();
    addLabels(pCounterexample, cfa);
  }

  private void addLabels(FormulaEntryList pCounterexample, CFA pCfa) {

    FormulaEntryList withoutPrecond = new FormulaEntryList(pCounterexample);
    if (withoutPrecond.isEmpty()) {
      return;
    }
    withoutPrecond.remove(0);

    MergePoint<CFANode> mergePoints =
        new MergePoint<>(
            pCfa.getMainFunction().getExitNode(), CFAUtils::successorsOf, CFAUtils::predecessorsOf);

    List<CFANode> path =
        withoutPrecond.toEdgeList().stream()
            .map(e -> e.getPredecessor())
            .collect(Collectors.toList());
    List<List<FormulaLabel>> labels = new ArrayList<>();
    for (int i = 0; i < path.size(); i++) {
      labels.add(new ArrayList<>());
    }
    for (int i = 0; i < path.size(); i++) {
      CFANode node = path.get(i);
      if (isAssumeNode(node)) {
        labels.get(i).add(FormulaLabel.IF);
        CFANode mergepoint = mergePoints.findMergePoint(node);
        for (int j = i; j < path.size(); j++) {
          if (path.get(j).equals(mergepoint)) {
            labels.get(j).add(FormulaLabel.ENDIF);
            break;
          }
        }
      }
    }

    clear();
    for (int i = 0; i < path.size(); i++) {
      LabeledFormula formula = new LabeledFormula(withoutPrecond.get(i));
      labels.get(i).forEach(formula::addLabel);
      add(formula);
    }
  }

  private boolean isAssumeNode(CFANode node) {
    if (node.getNumLeavingEdges() == 0) {
      return false;
    }

    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(node)) {
      if (!cfaEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected List<LabeledFormula> delegate() {
    return annotatedCounterexample;
  }

  static class LabeledFormula {

    private FormulaEntry entry;
    private List<FormulaLabel> labels;

    /**
     * Adds a label to a FormulaEntry
     *
     * @param pEntry the corresponding entry
     */
    public LabeledFormula(FormulaEntry pEntry) {
      labels = new ArrayList<>();
      entry = pEntry;
    }

    public FormulaEntry getEntry() {
      return entry;
    }

    public void addLabel(FormulaLabel pLabel) {
      labels.add(pLabel);
    }

    public List<FormulaLabel> getLabels() {
      return labels;
    }

    @Override
    public boolean equals(Object pO) {
      if (pO instanceof LabeledFormula) {
        LabeledFormula that = (LabeledFormula) pO;
        return entry.equals(that.entry) && labels.equals(that.labels);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(entry, labels);
    }

    @Override
    public String toString() {
      String labelString = labels.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      return "LabeledFormula{" + "entry=" + entry + ", labels=" + labelString + '}';
    }
  }
}
