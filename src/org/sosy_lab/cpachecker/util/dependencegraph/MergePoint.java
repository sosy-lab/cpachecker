// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

public class MergePoint<T> {

  private DomTree<T> tree;
  private Function<T, Iterable<T>> actualSuccessors;

  /**
   * Find the first merge point of a node with two successors. E.g. find the node that represents an
   * endif statement for a given assume node.
   *
   * @param pExitNode the last node of the underlying graph
   * @param pSuccessors a function that calculates all successors of a given node.
   * @param pPredecessors a function that calculates all predecessors of a given node.
   */
  public MergePoint(
      T pExitNode, Function<T, Iterable<T>> pSuccessors, Function<T, Iterable<T>> pPredecessors) {
    tree = Dominance.createDomTree(pExitNode, pPredecessors, pSuccessors);
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
    Set<Integer> potentialMergeNodes = new HashSet<>();
    List<List<Integer>> waitlist = new ArrayList<>();
    int assumeId = tree.getId(assume);

    for (T succ : actualSuccessors.apply(assume)) {
      List<Integer> waitlistElem = new ArrayList<>();
      waitlistElem.add(tree.getId(succ));
      waitlist.add(waitlistElem);
    }

    if (waitlist.isEmpty()) {
      throw new AssertionError("an assume edge must have branching edges");
    }

    // breadth first search for merge points
    int lastElement = -1;
    List<Integer> currentPath;
    while (!waitlist.isEmpty()) {

      currentPath = waitlist.remove(0);

      lastElement = currentPath.remove(currentPath.size() - 1);

      if (isPotentialMergeNode(assumeId, lastElement, currentPath)) {
        int prevSize = potentialMergeNodes.size();
        potentialMergeNodes.add(lastElement);
        // if current element is already member of merge points we found a merge point.
        if (prevSize == potentialMergeNodes.size()) {
          return tree.getNode(lastElement);
        }
      }
      currentPath.add(lastElement);
      for (T succ : Objects.requireNonNull(actualSuccessors.apply(tree.getNode(lastElement)))) {
        int succId = tree.getId(succ);
        List<Integer> newPath = new ArrayList<>(currentPath);
        newPath.add(succId);
        waitlist.add(newPath);
      }
    }
    return tree.getNode(lastElement);
  }

  private boolean isPotentialMergeNode(int assume, int test, List<Integer> path) {
    Set<Integer> postDominators = getPostDominators(assume);
    if (postDominators.contains(test)) {
      for (int i : path) {
        if (postDominators.contains(i)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Calculate dominators of a certain node
   *
   * @param id the id of a node
   * @return the post-dominators of the node with a certain <code>id</code>
   */
  private Set<Integer> getPostDominators(int id) {
    Set<Integer> dominators = new HashSet<>();

    while (tree.hasParent(id)) {
      id = tree.getParent(id);
      dominators.add(id);
    }

    return dominators;
  }
}
