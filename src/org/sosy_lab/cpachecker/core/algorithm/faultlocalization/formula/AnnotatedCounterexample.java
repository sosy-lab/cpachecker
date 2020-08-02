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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.AnnotatedCounterexample.FormulaNode;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaEntryList.FormulaEntry;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;

final class AnnotatedCounterexample extends ForwardingList<FormulaNode> {

  enum FormulaLabel {
    IF, ENDIF, OTHER
  }

  private List<FormulaNode> annotatedCounterexample;

  /**
   * The AnnotatedCounterexample adds the labels IF, ENDIF and OTHER to every statement in the counterexample
   * @param pCounterexample the counterexample for which we want to compute the labels
   * @param pContext the context
   */
  public AnnotatedCounterexample(FormulaEntryList pCounterexample, FormulaContext pContext) {
    annotatedCounterexample = new ArrayList<>();
    CFA cfa = pContext.getMutableCFA();
    addLabels(pCounterexample, cfa);
  }

  private void addLabels(FormulaEntryList pCounterexample, CFA pCfa) {
    if (pCounterexample.isEmpty()) {
      return;
    }

    Preconditions.checkState(
        pCfa.getDependenceGraph().isPresent(),
        "to use the improved version of the error invariants algorithm please enable cfa.createDependenceGraph with -setprop");
    DependenceGraph graph = pCfa.getDependenceGraph().get();
   /* DomTree<CFAEdge>
        tree = Dominance.createDomTree(pCounterexample.get(0).getSelector().getEdge(), l -> {
      try {
        return graph.getReachable(l, TraversalDirection.FORWARD);
      } catch (InterruptedException pE) {
        return null;
      }
    }, l -> l.getSuc);

    Dominance.createDomTraversable()*/

    FormulaNode prev = null;
    for (FormulaEntry entry : pCounterexample) {
      FormulaLabel label;
      if (entry.getSelector().getEdge().getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        label = FormulaLabel.IF;
      } else {
        // if one node less then before && prev != null:
        prev.setLabel(FormulaLabel.ENDIF);
        // else
        label = FormulaLabel.OTHER;
      }
      FormulaNode node = new FormulaNode(label, entry);
      annotatedCounterexample.add(node);
      prev = node;
    }
  }

  @Override
  protected List<FormulaNode> delegate() {
    return annotatedCounterexample;
  }


  class FormulaNode {

    private FormulaEntry entry;
    private FormulaLabel label;

    /**
     * Adds a label to an FormulaEntry
     * @param pLabel a label for the entry
     * @param pEntry the corresponding entry
     */
    public FormulaNode(FormulaLabel pLabel, FormulaEntry pEntry) {
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
      if (!(pO instanceof FormulaNode)) {
        return false;
      }
      FormulaNode that = (FormulaNode) pO;
      return label == that.label &&
          Objects.equals(entry, that.entry);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entry, label);
    }
  }


}
