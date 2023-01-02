// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public class MergePoint<T> {

  private DomTree<T> tree;
  private SuccessorsFunction<T> actualSuccessors;

  /**
   * Find the first merge point of a node with two successors. E.g. find the node that represents an
   * endif statement for a given assume node.
   *
   * @param pExitNode the last node of the underlying graph
   * @param pSuccessors a function that calculates all successors of a given node.
   * @param pPredecessors a function that calculates all predecessors of a given node.
   */
  public MergePoint(
      T pExitNode, SuccessorsFunction<T> pSuccessors, PredecessorsFunction<T> pPredecessors) {
    // FIXME: make it more obvious that successor and predecessor functions are switched
    tree = DomTree.forGraph(pSuccessors::successors, pPredecessors::predecessors, pExitNode);
    actualSuccessors = pSuccessors;
  }

  /**
   * Find the merge node of a given assume node. A merge point has two properties: it post-dominates
   * <code>T assume</code> and no element of all paths from <code>T assume</code> to the merge point
   * do not post-dominate <code>T assume</code>.
   *
   * @param assume an arbitrary assume node
   * @return the merge point of a given assume node
   */
  public T findMergePoint(T assume) {
    Set<T> potentialMergeNodes = new HashSet<>();
    List<List<T>> waitlist = new ArrayList<>();

    for (T succ : actualSuccessors.successors(assume)) {
      List<T> waitlistElem = new ArrayList<>();
      waitlistElem.add(succ);
      waitlist.add(waitlistElem);
    }

    if (waitlist.isEmpty()) {
      throw new AssertionError("an assume edge must have branching edges");
    }

    // breadth first search for merge points
    @Nullable T lastElement = null;
    List<T> currentPath;
    while (!waitlist.isEmpty()) {

      currentPath = waitlist.remove(0);

      lastElement = currentPath.remove(currentPath.size() - 1);

      if (isPotentialMergeNode(assume, lastElement, currentPath)) {
        int prevSize = potentialMergeNodes.size();
        potentialMergeNodes.add(lastElement);
        // if current element is already member of merge points we found a merge point.
        if (prevSize == potentialMergeNodes.size()) {
          return lastElement;
        }
      }
      currentPath.add(lastElement);
      for (T succ : Objects.requireNonNull(actualSuccessors.successors(lastElement))) {
        List<T> newPath = new ArrayList<>(currentPath);
        newPath.add(succ);
        waitlist.add(newPath);
      }
    }
    return lastElement;
  }

  private boolean isPotentialMergeNode(T assume, T test, List<T> path) {
    Set<T> postDominators = tree.getAncestors(assume);
    if (postDominators.contains(test)) {
      for (T n : path) {
        if (postDominators.contains(n)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
