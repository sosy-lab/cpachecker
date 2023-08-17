// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public class SubsumptionOracle {

  private final DomTree<CFANode> domTree;
  private final DomTree<CFANode> inverseDomTree;
  private final ImmutableSet<CFAEdge> reachedFromExit;
  private final Map<CFAEdge, CFAEdge> originalTargetToCopiedTarget;

  public SubsumptionOracle(
      final Pair<CFANode, CFANode> pEntryExit, final Map<CFAEdge, CFAEdge> pCopyToTarget) {
    originalTargetToCopiedTarget = Maps.newHashMapWithExpectedSize(pCopyToTarget.size());
    for (Entry<CFAEdge, CFAEdge> entry : pCopyToTarget.entrySet()) {
      originalTargetToCopiedTarget.put(entry.getValue(), entry.getKey());
    }
    domTree =
        DomTree.forGraph(
            CFAUtils::allPredecessorsOf, CFAUtils::allSuccessorsOf, pEntryExit.getFirst());
    inverseDomTree =
        pEntryExit.getSecond() != null
            ? DomTree.forGraph(
                CFAUtils::allSuccessorsOf, CFAUtils::allPredecessorsOf, pEntryExit.getSecond())
            : null;

    if (pEntryExit.getSecond() == null) {
      reachedFromExit = ImmutableSet.empty();
    } else {
      reachedFromExit = getReachableFromExit(pCopyToTarget, pEntryExit.getSecond());
    }
  }

  private ImmutableSet<CFAEdge> getReachableFromExit(
      final Map<CFAEdge, CFAEdge> pCopyToTarget, final CFANode pCopiedExit) {
    Set<CFAEdge> reachableTargets = new HashSet<>(pCopyToTarget.size());
    Set<CFANode> visited = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    visited.add(pCopiedExit);
    waitlist.add(pCopiedExit);

    CFANode succ;
    while (!waitlist.isEmpty()) {
      succ = waitlist.poll();
      for (CFANode pred : CFAUtils.allPredecessorsOf(succ)) {
        if (visited.add(pred)) {
          waitlist.add(pred);
        }
      }
    }

    for (Entry<CFAEdge, CFAEdge> mapEntry : pCopyToTarget.entrySet()) {
      if (visited.contains(mapEntry.getKey().getSuccessor())) {
        reachableTargets.add(mapEntry.getValue());
      }
    }

    return ImmutableSet.of(reachableTargets);
  }

  public boolean subsumes(final CFAEdge origEdgeSubsumer, final CFAEdge origEdgeSubsumed) {
    Preconditions.checkArgument(originalTargetToCopiedTarget.containsKey(origEdgeSubsumer));
    Preconditions.checkArgument(originalTargetToCopiedTarget.containsKey(origEdgeSubsumed));

    // TODO currently only approximation via dominator trees on nodes, not on edges
    return origEdgeSubsumer.getSuccessor().getNumEnteringEdges() == 1
        && origEdgeSubsumed.getSuccessor().getNumEnteringEdges() == 1
        // origEdgeSubsumed is ancestor/dominator of origEdgeSubsumer
        && (domTree.isAncestorOf(
                originalTargetToCopiedTarget.get(origEdgeSubsumed).getSuccessor(),
                originalTargetToCopiedTarget.get(origEdgeSubsumer).getSuccessor())
            || (inverseDomTree != null
                && reachedFromExit.contains(origEdgeSubsumed)
                && reachedFromExit.contains(origEdgeSubsumer)
                && inverseDomTree.isAncestorOf(
                    originalTargetToCopiedTarget.get(origEdgeSubsumed).getSuccessor(),
                    originalTargetToCopiedTarget.get(origEdgeSubsumer).getSuccessor())));
    /*
     * Implementation of Arcs subsumes?. An arc e subsumes an arc e’ if every path from the
     * entry arc to e contains e’ or else if every path from e to the exit arc contains e’
     * [4], i.e., if AL(eo,e’,e) or AL(e,e’,e~).
     */
  }
}
