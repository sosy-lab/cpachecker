// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import com.google.common.collect.ForwardingList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.LabeledCounterexample.LabeledFormula;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;
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
   * The LabeledCounterexample adds the labels IF, ENDIF, BOTH and OTHER to every statement in the counterexample
   * @param pCounterexample the counterexample for which we want to compute the labels
   *
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

    DomTree<CFANode> tree = Dominance.createDomTree(pCfa.getMainFunction(),
        node -> CFAUtils.successorsOf(node),
        node -> CFAUtils.predecessorsOf(node));


    MergePoint<CFANode> mergePoints = new MergePoint<>(tree, node -> CFAUtils.successorsOf(node), node -> isAssumeNode(node));

    List<CFANode> path = withoutPrecond.toEdgeList().stream().map(e -> e.getPredecessor()).collect(
        Collectors.toList());
    List<CFANode> endifs = new ArrayList<>();
    path.forEach(node -> {
      if (isAssumeNode(node)) {
        endifs.add(mergePoints.findMergePoint(node));
      }
    });

    Map<CFANode, Integer> depth = new HashMap<>();
    for (CFANode endif : endifs) {
      depth.merge(endif, 1, Integer::sum);
    }

    Map<CFAEdge, LabeledFormula> edgeToLabeledFormula = new HashMap<>();
    withoutPrecond.forEach(entry -> edgeToLabeledFormula.put(entry.getSelector().getEdge(), new LabeledFormula(entry)));

    for (CFANode node : path) {
      if (isAssumeNode(node)) {
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge edge = node.getLeavingEdge(i);
          if (edgeToLabeledFormula.containsKey(edge)) {
            edgeToLabeledFormula.get(edge).addLabel(FormulaLabel.IF);
            break;
          }
        }
      }

      if (depth.containsKey(node)) {
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge edge = node.getLeavingEdge(i);
          if (edgeToLabeledFormula.containsKey(edge)) {
            for (int j = 0; j < depth.get(node); j++){
              edgeToLabeledFormula.get(edge).getLabels().add(0, FormulaLabel.ENDIF);
            }
            break;
          }
        }
      }

    }

    annotatedCounterexample = withoutPrecond.toEdgeList().stream().map(edge -> edgeToLabeledFormula.get(edge)).collect(
        Collectors.toList());
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
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      LabeledFormula that = (LabeledFormula) pO;
      return entry.equals(that.entry) &&
          labels.equals(that.labels);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entry, labels);
    }

    @Override
    public String toString() {
      String labelString = labels.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      return "LabeledFormula{" +
          "entry=" + entry +
          ", labels=" + labelString +
          '}';
    }
  }

}
