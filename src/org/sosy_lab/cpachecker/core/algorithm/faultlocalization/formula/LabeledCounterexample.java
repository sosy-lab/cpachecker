// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.LabeledCounterexample.LabeledFormula;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

final class LabeledCounterexample extends ForwardingList<LabeledFormula> {

  enum FormulaLabel {
    IF, ENDIF, BOTH, OTHER
  }

  private List<LabeledFormula> annotatedCounterexample;

  /**
   * The LabeledCounterexample adds the labels IF, ENDIF and OTHER to every statement in the counterexample
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
    withoutPrecond.remove(0);
    if (withoutPrecond.isEmpty()) {
      return;
    }

    Preconditions.checkState(
        pCfa.getDependenceGraph().isPresent(),
        "to use the improved version of the error invariants algorithm please enable cfa.createDependenceGraph with -setprop");

    CFAEdge root = withoutPrecond.get(0).getSelector().getEdge();
    DomTree<CFAEdge> tree = Dominance.createDomTree(root,
        edge -> CFAUtils.allLeavingEdges(edge.getSuccessor()),
        edge -> CFAUtils.allEnteringEdges(edge.getPredecessor()));

    DomTreeHandler handler = new DomTreeHandler(tree);
    boolean endif;
    LabeledFormula prev = null;
    for (FormulaEntry entry : withoutPrecond) {
      // set OTHER as default label
      CFAEdge treeNode = entry.getSelector().getEdge();
      endif = handler.addWithNotificationIfMerged(treeNode);
      FormulaLabel label = FormulaLabel.OTHER;

      // if current labeledFormula is out of the if-block, the previous labeledFormula was an ENDIF statement
      if (prev != null && endif) {
        if (prev.label.equals(FormulaLabel.IF)) {
          prev.setLabel(FormulaLabel.BOTH);
        } else {
          prev.setLabel(FormulaLabel.ENDIF);
        }
      }

      // all assume edges are labeled as IF
      if (treeNode.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        label = FormulaLabel.IF;
      }

      LabeledFormula labeledFormula = new LabeledFormula(label, entry);
      annotatedCounterexample.add(labeledFormula);
      prev = labeledFormula;
    }
  }

  @Override
  protected List<LabeledFormula> delegate() {
    return annotatedCounterexample;
  }


  static class DomTreeHandler {
    private ArrayDeque<List<Integer>> stack;
    private DomTree<CFAEdge> tree;

    DomTreeHandler(DomTree<CFAEdge> pTree) {
      stack = new ArrayDeque<>();
      tree = pTree;
    }

    public boolean addWithNotificationIfMerged(CFAEdge edge) {
      if (stack.isEmpty()) {
        if (!edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
          return false;
        }
        split(edge);
        return false;
      }

      int id = tree.getId(edge);
      List<Integer> currPath = stack.peek();
      Set<Integer> dominators = getDominators(id);
      // 2 = root element and at least one statement
      if (currPath.size() >= 2) {
        if (dominators.contains(currPath.get(0)));
        for (int i = 1; i < currPath.size(); i++) {
          if (dominators.contains(currPath.get(i))) {
            if (edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
              split(edge);
            } else {
              currPath.add(id);
            }
            return false;
          }
        }
        merge();
        addWithNotificationIfMerged(edge);
        return true;
      }
      if (edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        split(edge);
      } else {
        currPath.add(id);
      }
      return false;
    }

    public void split(CFAEdge edge){
      int id = tree.getId(edge);
      List<Integer> currPath = new ArrayList<>();
      currPath.add(id);
      stack.push(currPath);
    }

    public void merge() {
      List<Integer> currPath = stack.pop();
      if (!stack.isEmpty()) {
        stack.peek().addAll(currPath);
      }
    }

    private Set<Integer> getDominators(int id) {
      Set<Integer> dominators = new HashSet<>();

      while(tree.hasParent(id)) {
        id = tree.getParent(id);
        dominators.add(id);
      }

      return dominators;
    }

  }

  static class LabeledFormula {

    private FormulaEntry entry;
    private FormulaLabel label;

    /**
     * Adds a label to an FormulaEntry
     * @param pLabel a label for the entry
     * @param pEntry the corresponding entry
     */
    public LabeledFormula(FormulaLabel pLabel, FormulaEntry pEntry) {
      label = pLabel;
      entry = pEntry;
    }

    public void setLabel(FormulaLabel pLabel) {
      label = pLabel;
    }

    public FormulaEntry getEntry() {
      return entry;
    }

    public FormulaLabel getLabel() {
      return label;
    }

    @Override
    public boolean equals(Object pO) {
      if (!(pO instanceof LabeledFormula)) {
        return false;
      }
      LabeledFormula that = (LabeledFormula) pO;
      return label == that.label &&
          Objects.equals(entry, that.entry);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entry, label);
    }

    @Override
    public String toString() {
      return "LabeledFormula{" +
          "label=" + label +
          ", entry=" + entry +
          '}';
    }
  }


}
