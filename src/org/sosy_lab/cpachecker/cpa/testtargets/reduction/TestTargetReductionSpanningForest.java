// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.testtargets.reduction.TestTargetReductionUtils.CFAEdgeNode;
import org.sosy_lab.cpachecker.util.Pair;

public class TestTargetReductionSpanningForest {
  public Set<CFAEdge> reduceTargets(final Set<CFAEdge> pTargets, final CFA pCfa) {
    Preconditions.checkNotNull(pTargets);

    Map<CFAEdge, CFAEdge> copyToTarget = Maps.newHashMapWithExpectedSize(pTargets.size());
    Pair<CFANode, CFANode> entryExit =
        TestTargetReductionUtils.buildTestGoalGraph(pTargets, copyToTarget, pCfa.getMainFunction());

    Set<CFAEdge> visited = Sets.newHashSetWithExpectedSize(pTargets.size());
    Map<CFAEdge, CFAEdgeNode> targetsAsNodes = Maps.newHashMapWithExpectedSize(pTargets.size());

    for (CFAEdge target : pTargets) {
      targetsAsNodes.put(target, new CFAEdgeNode(target));
    }

    SubsumptionOracle oracle = new SubsumptionOracle(entryExit, copyToTarget);

    for (CFAEdge target : pTargets) {
      if (visited.contains(target)) {
        for (CFAEdge target2 : pTargets) {
          if (target == target2 || visited.contains(target2)) {
            continue;
          }
          // TODO currently only approximation via dominator trees on nodes, not on edges
          if (oracle.subsumes(target, target2)) { // target subsumes target2
            targetsAsNodes.get(target).addEdgeTo(targetsAsNodes.get(target2));
            visited.add(target2);
          }
        }
      }
    }
    return getRootNodes(targetsAsNodes.values());
  }

  private Set<CFAEdge> getRootNodes(final Collection<CFAEdgeNode> forestNodes) {
    return new HashSet<>(
        FluentIterable.from(forestNodes)
            .filter(CFAEdgeNode::isRoot)
            .transform(node -> node.getRepresentedEdge())
            .toSet());
  }
}
