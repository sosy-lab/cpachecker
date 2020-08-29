// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

public class MergePoint<T> {

    private DomTree<T> tree;
    private Function<T, Iterable<T>> successor;
    private Predicate<T> isAssumeNode;

    private Set<T> cache;

  /**
   * Find the first merge point of a node with two successors.
   * E.g. find the node that represents an endif statement for a given assume node.
   * @param pTree An existing tree to calculate the dominators
   * @param pSuccessor a function that calculates all successors of a given node.
   */
    public MergePoint(DomTree<T> pTree, Function<T, Iterable<T>> pSuccessor, Predicate<T> pIsAssumeNode) {
      cache = new HashSet<>();
      tree = pTree;
      successor = pSuccessor;
      isAssumeNode = pIsAssumeNode;
      fillCache();
    }

  /**
   * Find the merge node of a given assume node.
   * A merge point has two properties: the node is dominated by <code>T assume</code>
   * and all elements of all paths from <code>T assume</code> to the merge point do not dominate
   * the merge point.
   * @param assume an arbitrary assume node
   * @return the merge point of a given assume node
   */
    public T findMergePoint(T assume) {
      Preconditions.checkArgument(isAssumeNode.test(assume), "findMergePoint only accepts assume nodes as input");
      Set<Integer> potentialMergeNodes = new HashSet<>();
      List<List<Integer>> waitlist = new ArrayList<>();
      int assumeId = tree.getId(assume);

      Objects.requireNonNull(successor.apply(assume)).forEach(e -> waitlist.add(new ArrayList<>(ImmutableList.of(tree.getId(e)))));

      int left = waitlist.get(0).get(0);

      if (waitlist.isEmpty()) {
        throw new AssertionError("an assume edge must have branching edges");
      }

      //breadth first search for merge points
      int lastElement = -1;
      List<Integer> currentPath;
      while (!waitlist.isEmpty()) {

        Optional<T> intercept = intercept(waitlist, left);
        if (intercept.isPresent()) {
          return intercept.get();
        }

        currentPath = waitlist.remove(0);

        lastElement = currentPath.remove(currentPath.size()-1);

        if (isPotentialMergeNode(assumeId, lastElement, currentPath)) {
          int prevSize = potentialMergeNodes.size();
          potentialMergeNodes.add(lastElement);
          // if current element is already member of merge points we found a merge point.
          if (prevSize == potentialMergeNodes.size()) {
            T mergePoint = tree.getNode(lastElement);
            cache.add(mergePoint);
            return mergePoint;
          }
        }
        currentPath.add(lastElement);
        for (T succ : Objects.requireNonNull(successor.apply(tree.getNode(lastElement)))) {
          int succId = tree.getId(succ);
          List<Integer> newPath = new ArrayList<>(currentPath);
          newPath.add(succId);
          waitlist.add(newPath);
        }
      }
      T mergePoint = tree.getNode(lastElement);
      cache.add(mergePoint);
      return mergePoint;
    }

    private Optional<T> intercept(List<List<Integer>> waitlist, int left) {

      Set<Integer> rightNumbers = new HashSet<>();
      Set<Integer> leftNumbers = new HashSet<>();

      for (List<Integer> path : waitlist) {
        if (path.get(0) == left) {
          leftNumbers.addAll(path);
        } else {
          rightNumbers.addAll(path);
        }
      }

      rightNumbers.retainAll(leftNumbers);
      rightNumbers.retainAll(cache.stream().map(t -> tree.getId(t)).collect(Collectors.toSet()));

      if (!rightNumbers.isEmpty()) {
        return Optional.of(tree.getNode(rightNumbers.iterator().next()));
      }

      return Optional.empty();
    }

    private boolean isPotentialMergeNode(int assume, int test, List<Integer> path) {
      Set<Integer> dominators = getDominators(test);
      if (dominators.contains(assume)) {
        for (int i : path) {
          if (dominators.contains(i)) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    private void fillCache() {
      for (int i = 0; i < tree.getNodeCount(); i++) {
        T currNode = tree.getNode(i);
        if (isAssumeNode.test(currNode)) {
          findMergePoint(currNode);
        }
      }
    }

    /**
     * Calculate dominators of a certain node
     *
     * @param id the id of a node
     * @return the dominators of the node referred to the given id
     */
    private Set<Integer> getDominators(int id) {
      Set<Integer> dominators = new HashSet<>();

      while (tree.hasParent(id)) {
        id = tree.getParent(id);
        dominators.add(id);
      }

      return dominators;
    }
}
