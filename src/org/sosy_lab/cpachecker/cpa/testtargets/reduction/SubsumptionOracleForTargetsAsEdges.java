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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

public class SubsumptionOracleForTargetsAsEdges {

  private final DomTree<CFANode> domTree;
  private final DomTree<CFANode> inverseDomTree;
  private final ImmutableSet<CFAEdge> reachedFromExit;
  private final ImmutableSet<CFAEdge> reachedFromEntry;
  private final ImmutableSet<Pair<CFAEdge, CFAEdge>> reachableViaInputs;
  private final Map<CFAEdge, CFAEdge> originalTargetToCopiedTarget;

  public SubsumptionOracleForTargetsAsEdges(
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

    reachedFromEntry = getReachableFromEntry(pCopyToTarget, pEntryExit.getFirst());
    if (pEntryExit.getSecond() == null) {
      reachedFromExit = ImmutableSet.of();
    } else {
      reachedFromExit = getReachableFromExit(pCopyToTarget, pEntryExit.getSecond());
    }

    reachableViaInputs = determinePathsWithRequiredInputs(pCopyToTarget.keySet());
  }

  private ImmutableSet<Pair<CFAEdge, CFAEdge>> determinePathsWithRequiredInputs(
      Collection<CFAEdge> pCopiedTargets) {
    Map<Pair<CFAEdge, CFAEdge>, Boolean> pathsToRequiredInputs = new HashMap<>();

    Deque<Pair<CFAEdge, CFAEdge>> waitlist = new ArrayDeque<>();
    Pair<CFAEdge, CFAEdge> path;
    Pair<CFAEdge, CFAEdge> newPath;
    boolean viaInput;

    for (CFAEdge predTarget : pCopiedTargets) {
      for (CFAEdge leaving : CFAUtils.allLeavingEdges(predTarget.getSuccessor())) {
        if (pCopiedTargets.contains(leaving)) {
          if (predTarget.equals(leaving)) {
            continue;
          }
          newPath = Pair.of(predTarget, leaving);
          pathsToRequiredInputs.put(newPath, false);
          waitlist.add(newPath);
        } else {
          for (CFAEdge secDescend : CFAUtils.allLeavingEdges(leaving.getSuccessor())) {
            Preconditions.checkState(pCopiedTargets.contains(secDescend));

            newPath = Pair.of(predTarget, secDescend);
            pathsToRequiredInputs.put(newPath, TestTargetReductionUtils.isInputEdge(leaving));
            waitlist.add(newPath);
          }
        }
      }
    }

    while (!waitlist.isEmpty()) {
      path = waitlist.pop();
      for (CFAEdge leaving : CFAUtils.allLeavingEdges(path.getSecond().getSuccessor())) {
        if (pCopiedTargets.contains(leaving)) {
          newPath = Pair.of(path.getFirst(), leaving);

          viaInput = pathsToRequiredInputs.get(path);
          if (!pathsToRequiredInputs.containsKey(newPath)
              || (!pathsToRequiredInputs.get(newPath) && viaInput)) {
            pathsToRequiredInputs.put(newPath, viaInput);
            waitlist.add(newPath);
          }
          pathsToRequiredInputs.put(newPath, TestTargetReductionUtils.isInputEdge(leaving));
          waitlist.add(newPath);
        } else {
          for (CFAEdge secDescend : CFAUtils.allLeavingEdges(leaving.getSuccessor())) {
            Preconditions.checkState(pCopiedTargets.contains(secDescend));
            newPath = Pair.of(path.getFirst(), secDescend);

            viaInput =
                pathsToRequiredInputs.get(path) || TestTargetReductionUtils.isInputEdge(leaving);
            if (!pathsToRequiredInputs.containsKey(newPath)
                || (!pathsToRequiredInputs.get(newPath) && viaInput)) {
              pathsToRequiredInputs.put(newPath, viaInput);
              waitlist.add(newPath);
            }
          }
        }
      }
    }
    return ImmutableSet.copyOf(
        FluentIterable.from(pathsToRequiredInputs.keySet())
            .filter(pathPair -> pathsToRequiredInputs.get(pathPair)));
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

    return ImmutableSet.copyOf(reachableTargets);
  }

  private ImmutableSet<CFAEdge> getReachableFromEntry(
      final Map<CFAEdge, CFAEdge> pCopyToTarget, final CFANode pCopiedEntry) {
    Set<CFAEdge> reachableTargets = new HashSet<>(pCopyToTarget.size());
    Set<CFANode> visited = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    visited.add(pCopiedEntry);
    waitlist.add(pCopiedEntry);

    CFANode pred;
    while (!waitlist.isEmpty()) {
      pred = waitlist.poll();
      for (CFANode succ : CFAUtils.allSuccessorsOf(pred)) {
        if (visited.add(succ)) {
          waitlist.add(succ);
        }
      }
    }

    for (Entry<CFAEdge, CFAEdge> mapEntry : pCopyToTarget.entrySet()) {
      if (visited.contains(mapEntry.getKey().getPredecessor())) {
        reachableTargets.add(mapEntry.getValue());
      }
    }

    return ImmutableSet.copyOf(reachableTargets);
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
                    originalTargetToCopiedTarget.get(origEdgeSubsumer).getSuccessor())
                // check that origEdgeSubsumer is reachable from program entry, TODO required?
                && reachedFromEntry.contains(origEdgeSubsumer)
                /* check that no input required on any path from origEdgeSubsumer to origEdgeSubsumed */
                && !reachableViaInputs.contains(
                    Pair.of(
                        originalTargetToCopiedTarget.get(origEdgeSubsumer),
                        originalTargetToCopiedTarget.get(origEdgeSubsumed)))));

    /*
     * Implementation of Arcs subsumes?. An arc e subsumes an arc e’ if every path from the
     * entry arc to e contains e’ or else if every path from e to the exit arc contains e’
     * [4], i.e., if AL(eo,e’,e) or AL(e,e’,e~).
     */
  }
}
