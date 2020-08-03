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
    // assume edges are IF statements
    IF,
    // edges that are the last statement of an if-block are ENDIF statements
    ENDIF,
    // edges that have both of the above properties are BOTH statements
    BOTH,
    // edges that do not belong to any of the above categories are OTHER statements
    OTHER
  }

  private List<LabeledFormula> annotatedCounterexample;

  /**
   * The LabeledCounterexample adds the labels IF, ENDIF, BOTH and OTHER to every statement in the counterexample
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
      CFAEdge treeNode = entry.getSelector().getEdge();

      // check if previous node ended an if-statement
      endif = handler.addWithNotificationIfMerged(treeNode);

      // set OTHER as default label
      FormulaLabel label = FormulaLabel.OTHER;

      // if current labeledFormula is not part of the if-block anymore,
      // the previous labeledFormula was an ENDIF statement
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

    /**
     * Handles the addition of new edges to the paths
     * @param edge edge to be processed
     * @return true if the edge caused a merge operation, false else
     */
    public boolean addWithNotificationIfMerged(CFAEdge edge) {
      // create a new path on the first if statement, else ignore the edge
      if (stack.isEmpty()) {
        if (!edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
          return false;
        }
        split(edge);
        return false;
      }

      // extract the id of the given edge
      int id = tree.getId(edge);
      List<Integer> currPath = stack.peek();
      Set<Integer> dominators = getDominators(id);
      // 2 = root element and at least one statement
      if (currPath.size() >= 2) {
        // the current node has potential to end an if statement
        // we have to check two conditions for that:
        // 1) it is dominated by the most recent assume edge
        // 2) no edge between the current edge and the most recent assume edge dominates the current edge
        if (dominators.contains(currPath.get(0))) {
          // 1) holds.
          // check every edge on the path.
          int DOMINATED = -1;
          int i;
          for (i = 1; i < currPath.size(); i++) {
            if (dominators.contains(currPath.get(i))) {
              i = DOMINATED;
              break;
            }
          }
          if (i != DOMINATED) {
            // 2) holds
            merge();
            // process the edge again.
            // we ignored that this edge may be an assume edge or member of the current most recent path
            addWithNotificationIfMerged(edge);
            return true;
          }
        }
      }
      // the edge is neither the first assume edge nor an ENDIF statement
      if (edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        // create a new path if the edge is an assume edge
        split(edge);
      } else {
        // add the edge to the most recent path otherwise
        currPath.add(id);
      }
      return false;
    }

    /**
     * Add a new path to the stack.
     * @param edge the path starts with this edge
     */
    private void split(CFAEdge edge){
      Preconditions.checkArgument(edge.getEdgeType().equals(CFAEdgeType.AssumeEdge), "the split is not performed on an assume edge");
      int id = tree.getId(edge);
      List<Integer> currPath = new ArrayList<>();
      currPath.add(id);
      stack.push(currPath);
    }

    /**
     * whenever an ENDIF-label is reached merge the most recent path with the previous path.
     */
    private void merge() {
      List<Integer> currPath = stack.pop();
      if (!stack.isEmpty()) {
        stack.peek().addAll(currPath);
      }
    }

    /**
     * Calculate dominators of a certain node
     * @param id the id of a node
     * @return the dominators of the node referred to the given id
     */
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
     * Adds a label to a FormulaEntry
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
