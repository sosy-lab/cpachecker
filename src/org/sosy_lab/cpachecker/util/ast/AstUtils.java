// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ast;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.Pair;

public class AstUtils {
  /**
   * This method computes the nodes which are at the boundary between a condition and the nodes
   * inside two branches starting from it. This can be used to find out after which node one of the
   * branches has been definitely been selected.
   *
   * <p>To do this, first every node in the boundary of the condition is computed. These are all
   * nodes which are successors of edges in the condition but not predecessors, we call these
   * boundary condition nodes. If the first branch contains at least one node, the
   * intersection/difference of nodes which are a predecessor of an edge in the first branch with
   * the boundary condition nodes is taken to get the nodes between the condition and the first
   * branch/second branch. If the first branch is empty, the intersection/difference of the boundary
   * condition nodes with the nodes which are a predecessor of an edge in the second branch is taken
   * to get the nodes between the condition and the first branch/second branch.
   *
   * <p>
   *
   * <p>For example, consider the following graph for an if condition, where node 6 and 5 are the
   * first nodes in the 'then' and 'else' branches respectively. Then the nodes at the boundary are
   * 3 and 4, where 3 is at the boundary for the 'then' branch and 4 is at the boundary to the
   * 'else' branch.
   *
   * <pre>
   *       1
   *      / \
   *     2---3
   *     |   |
   *     4   5
   *     |
   *     6
   * </pre>
   */
  static Pair<ImmutableSet<CFANode>, ImmutableSet<CFANode>> computeNodesConditionBoundaryNodes(
      Set<CFAEdge> pEdgesCondition,
      Optional<Set<CFAEdge>> pEdgesFirstBranch,
      Optional<Set<CFAEdge>> pEdgesSecondBranch) {
    final Set<CFANode> nodesBoundaryCondition =
        Sets.difference(
            transformedImmutableSetCopy(pEdgesCondition, CFAEdge::getSuccessor),
            transformedImmutableSetCopy(pEdgesCondition, CFAEdge::getPredecessor));
    final Set<CFANode> collectorNodesBetweenConditionAndSecondBranch;
    final Set<CFANode> collectorNodesBetweenConditionAndFirstBranch;

    if (pEdgesFirstBranch.isEmpty() && pEdgesSecondBranch.isEmpty()) {
      // TODO: Currently we over-approximate by taking both branches when there are no edges
      //  in both branches
      collectorNodesBetweenConditionAndFirstBranch = nodesBoundaryCondition;
      collectorNodesBetweenConditionAndSecondBranch = nodesBoundaryCondition;
    } else if (pEdgesFirstBranch.isEmpty() && pEdgesSecondBranch.isPresent()) {
      final Set<CFANode> nodesSecondBranch =
          transformedImmutableSetCopy(pEdgesSecondBranch.orElseThrow(), CFAEdge::getPredecessor);
      collectorNodesBetweenConditionAndFirstBranch =
          Sets.difference(nodesBoundaryCondition, nodesSecondBranch);
      collectorNodesBetweenConditionAndSecondBranch =
          Sets.intersection(nodesBoundaryCondition, nodesSecondBranch);
    } else if (pEdgesFirstBranch.isPresent() && pEdgesSecondBranch.isEmpty()) {
      final Set<CFANode> nodesFirstBranch =
          transformedImmutableSetCopy(pEdgesFirstBranch.orElseThrow(), CFAEdge::getPredecessor);
      collectorNodesBetweenConditionAndFirstBranch =
          Sets.intersection(nodesBoundaryCondition, nodesFirstBranch);
      collectorNodesBetweenConditionAndSecondBranch =
          Sets.difference(nodesBoundaryCondition, nodesFirstBranch);
    } else if (pEdgesFirstBranch.isPresent() && pEdgesSecondBranch.isPresent()) {
      final Set<CFANode> nodesFirstBranch =
          transformedImmutableSetCopy(pEdgesFirstBranch.orElseThrow(), CFAEdge::getPredecessor);
      final Set<CFANode> nodesSecondBranch =
          transformedImmutableSetCopy(pEdgesSecondBranch.orElseThrow(), CFAEdge::getPredecessor);
      collectorNodesBetweenConditionAndFirstBranch =
          Sets.intersection(nodesBoundaryCondition, nodesFirstBranch);
      collectorNodesBetweenConditionAndSecondBranch =
          Sets.intersection(nodesBoundaryCondition, nodesSecondBranch);
    } else {
      throw new AssertionError("Unexpected branch");
    }

    return Pair.of(
        ImmutableSet.copyOf(collectorNodesBetweenConditionAndFirstBranch),
        ImmutableSet.copyOf(collectorNodesBetweenConditionAndSecondBranch));
  }
}
