// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.TestGoalToConditionConverterAlgorithm.LeafStates;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Finds all un-/covered goals based on propagation.
 */
public class LeafGoalWithPropagationStrategy implements IGoalFindingStrategy {
  /**
   * Returns a map of (un-)/covered goals based on propagation. This means that first all
   * leaf nodes are calculated. If all of a node's children share the same state then the
   * parent is also in this state.
   *
   * @param pWaitlist    An initial list of ARGstates to check. Should be the exit node(s)
   *                     of the function.
   * @param coveredGoals A list of all covered goals (or rather their corresponding labels).
   * @return A map of (un-)/covered goals.
   */
  @Override
  public Map<LeafStates, List<CFANode>> findGoals(
      Deque<ARGState> pWaitlist,
      final Set<String> coveredGoals) {
    var waitList = pWaitlist.stream()
        .map(AbstractStates::extractLocation)
        .collect(Collectors.toCollection(ArrayDeque::new));
    Set<CFANode> reachedNodes = new HashSet<>();

    var nodes = new HashMap<NodeStates, HashSet<CFANode>>();
    nodes.put(NodeStates.VIRGIN, new HashSet<>());
    nodes.put(NodeStates.UNCOVERED, new HashSet<>());
    nodes.put(NodeStates.COVERED, new HashSet<>());

    var removableNodes = new HashSet<CFANode>();

    while (!waitList.isEmpty()) {
      var node = waitList.removeFirst();
      reachedNodes.add(node);

      ImmutableList<CFANode> childrenNodes = CFAUtils.successorsOf(node).toList();

      if (nodes.get(NodeStates.VIRGIN).containsAll(childrenNodes)
          && node instanceof CFALabelNode
          && ((CFALabelNode) node).getLabel().matches("^GOAL_[0-9]+$")) {
        if (coveredGoals.contains(((CFALabelNode) node).getLabel())) {
          nodes.get(NodeStates.COVERED).add(node);
        } else {
          nodes.get(NodeStates.UNCOVERED).add(node);
        }

      } else if (childrenNodes.isEmpty() || nodes.get(NodeStates.VIRGIN)
          .containsAll(childrenNodes)) {
        nodes.get(NodeStates.VIRGIN).add(node);
      } else if (nodes.get(NodeStates.COVERED).containsAll(childrenNodes)) {
        nodes.get(NodeStates.COVERED).add(node);
      } else if (nodes.get(NodeStates.UNCOVERED).containsAll(childrenNodes)) {
        nodes.get(NodeStates.UNCOVERED).add(node);
      } else {
        continue;
      }

      removableNodes.addAll(childrenNodes);

      ImmutableList<CFANode> todoNodes =
          CFAUtils.predecessorsOf(node)
              .filter(n -> !reachedNodes.contains(n) && !waitList.contains(n))
              .toList();
      waitList.addAll(todoNodes);
    }

    nodes.get(NodeStates.COVERED).removeAll(removableNodes);
    nodes.get(NodeStates.UNCOVERED).removeAll(removableNodes);

    Map<LeafStates, List<CFANode>> leafGoals = new HashMap<>();
    leafGoals.put(LeafStates.COVERED, new ArrayList<>(nodes.get(NodeStates.COVERED)));
    leafGoals.put(LeafStates.UNCOVERED, new ArrayList<>(nodes.get(NodeStates.UNCOVERED)));

    return leafGoals;
  }


  /**
   * All states a node can be in.
   */
  private enum NodeStates {
    /**
     * There was no label/ goal prior to this node and we don't know if it covered or not.
     */
    VIRGIN,

    /**
     * The node is uncovered.
     */
    UNCOVERED,

    /**
     * The node is covered.
     */
    COVERED,

    /**
     * Thid node's children are both covered and uncovered.
     */
    BOTH
  }
}
