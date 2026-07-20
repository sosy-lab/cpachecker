// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.composite.StraightLineBlockAggregator;
import org.sosy_lab.cpachecker.cpa.mutex.MutexFunctions;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;

class SingleGlobalStatementBlockAggregator extends StraightLineBlockAggregator {

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  private final ImmutableCollection<CFANode> initializationPhaseNodes;
  private final ImmutableCollection<CFANode> atomicBlockNodes;

  SingleGlobalStatementBlockAggregator(CFA pCfa) {
    super(pCfa);
    initializationPhaseNodes = getInitializationPhaseNodes(pCfa);
    atomicBlockNodes = getAtomicBlockNodes(pCfa);
  }

  @Override
  public boolean isValidMultiEdgeComponent(CFANode startNode, CFAEdge edge) {
    if (!super.isValidMultiEdgeComponent(startNode, edge)) {
      return false;
    }
    if (startNode.equals(edge.getPredecessor())) {
      return true;
    }
    if (isThreadStart(edge) || isThreadJoin(edge)) {
      return false;
    }
    if (initializationPhaseNodes.contains(edge.getPredecessor())) {
      return true;
    }

    boolean anyGlobalStatements = false;
    CFAEdge currentEdge = edge;
    while (true) {
      // A block must never extend past the end of a critical section: the node after an
      // atomic_end/unlock is a scheduling point where other threads may interleave, and the POR
      // dependence computation scopes a transition's footprint to at most one critical section
      // (see PORState#getUsedGlobalVars). Fusing `atomic_end; atomic_begin` into one step makes
      // two adjacent atomic blocks one atomic transition, silently removing the interleavings at
      // the boundary (wrong TRUE on the pthread-wmm litmus family, e.g. safe034: another thread's
      // z-write must fall between main's two atomic blocks to violate the assertion). The end
      // edge itself may close a block; anything walking back OVER an end edge is out.
      if (currentEdge != edge && MutexFunctions.isUnlockCall(currentEdge)) {
        return false;
      }
      // The same barrier applies to thread create/join edges anywhere along the walk, not only as
      // the candidate (line above the loop): the node after a pthread_create is a scheduling point
      // where the new thread is runnable. Fusing `pthread_create(P); atomic { guard = cnt == N }`
      // into one step removes every schedule in which the spawned threads run between the create
      // and the guard read — on the pthread-wmm tasks main's cnt==3 check then always fails, main
      // always aborts, and the assertion is unreachable on every explored path (wrong TRUE).
      if (currentEdge != edge && (isThreadStart(currentEdge) || isThreadJoin(currentEdge))) {
        return false;
      }
      // A lock/atomic_begin may only be the FIRST edge of a block: PORState#getUsedGlobalVars
      // extends a step's dependence footprint through the acquired critical section only when the
      // step STARTS with the lock call. A block like [blank; lock(G); ...] executes the
      // acquisition but carries the blank edge's empty footprint, so the reduction treats it as
      // independent of every other thread and prunes the schedules that interleave before the
      // acquisition (wrong TRUE on 28-race_reach_45-escape_racing: main's assert block must run
      // between t_fun's unlock(G) after (*p)++ and its re-lock of G before (*p)--).
      if (MutexFunctions.isLockCall(currentEdge)
          && !startNode.equals(currentEdge.getPredecessor())) {
        return false;
      }
      var accesses = memoryAccessExtractor.extract(currentEdge);
      if ((!accesses.getUses().isEmpty()
              || !accesses.getDefs().isEmpty()
              || !accesses.getPointeeDefs().isEmpty()
              || !accesses.getPointeeUses().isEmpty()
              // The walked edge (not the constant candidate) must be tested: a lock/atomic_begin
              // anywhere in the chain consumes the single-global-statement budget, so a block can
              // contain at most one critical-section entry and no global statement besides it.
              || MutexFunctions.isLockCall(currentEdge))
          && !atomicBlockNodes.contains(
              PorEdgeCloner.getOriginalNode(currentEdge.getPredecessor()))) {
        if (anyGlobalStatements) {
          return false;
        }
        anyGlobalStatements = true;
      }

      CFANode predecessor = currentEdge.getPredecessor();
      if (startNode.equals(predecessor)) {
        return true;
      }
      if (initializationPhaseNodes.contains(predecessor) && !anyGlobalStatements) {
        return true;
      }
      if (predecessor.getNumEnteringEdges() != 1) {
        // Walking back from `edge` did not reach `startNode` along a straight line: `predecessor`
        // is a merge point (or a function entry with no entering edge at all). A multi-edge block
        // has a single entry by definition, so `edge` is not a component of the one starting at
        // `startNode`. Branching inside an atomic block makes this reachable, so it must be a
        // plain answer rather than an assertion.
        return false;
      }
      currentEdge = predecessor.getEnteringEdges().iterator().next();
    }
  }

  private ImmutableCollection<CFANode> getInitializationPhaseNodes(CFA pCFA) {
    final Set<CFANode> nodesBeforeAnyThreadStart = new LinkedHashSet<>();
    final Set<CFANode> visitedNodes = new LinkedHashSet<>();
    final Set<CFANode> nodesToVisit = new LinkedHashSet<>();
    final Set<CFANode> threadStartNodes = new LinkedHashSet<>();

    // 1st traversal: add each node reachable from the initial node
    // continue until thread start edges
    nodesToVisit.add(pCFA.getMainFunction());
    while (!nodesToVisit.isEmpty()) {
      CFANode currentNode = nodesToVisit.iterator().next();
      nodesToVisit.remove(currentNode);
      if (!visitedNodes.add(currentNode)) {
        continue;
      }
      nodesBeforeAnyThreadStart.add(currentNode);
      for (CFAEdge edge : currentNode.getLeavingEdges()) {
        if (isThreadStart(edge)) {
          threadStartNodes.add(edge.getSuccessor());
        } else {
          nodesToVisit.add(edge.getSuccessor());
        }
      }
    }

    // 2nd traversal: remove all nodes reachable from thread start edges
    visitedNodes.clear();
    nodesToVisit.addAll(threadStartNodes);
    while (!nodesToVisit.isEmpty()) {
      CFANode currentNode = nodesToVisit.iterator().next();
      nodesToVisit.remove(currentNode);
      if (!visitedNodes.add(currentNode)) {
        continue;
      }
      nodesBeforeAnyThreadStart.remove(currentNode);
      for (CFAEdge edge : currentNode.getLeavingEdges()) {
        nodesToVisit.add(edge.getSuccessor());
      }
    }

    return transformedImmutableSetCopy(
        nodesBeforeAnyThreadStart, node -> PorEdgeCloner.getClonedNode(node, 0, pCFA));
  }

  private ImmutableCollection<CFANode> getAtomicBlockNodes(CFA pCFA) {
    ImmutableSet.Builder<CFANode> atomicBlockNodesBuilder = ImmutableSet.builder();
    for (CFAEdge edge : pCFA.edges()) {
      if (MutexFunctions.isAtomicBeginCall(edge)) {
        Set<CFANode> nodesToVisit = new LinkedHashSet<>();
        nodesToVisit.add(edge.getSuccessor());
        while (!nodesToVisit.isEmpty()) {
          CFANode currentNode = nodesToVisit.iterator().next();
          nodesToVisit.remove(currentNode);
          atomicBlockNodesBuilder.add(currentNode);
          for (CFAEdge leavingEdge : currentNode.getLeavingEdges()) {
            if (!MutexFunctions.isAtomicEndCall(leavingEdge)) {
              nodesToVisit.add(leavingEdge.getSuccessor());
            }
          }
        }
      }
    }
    return atomicBlockNodesBuilder.build();
  }

  private boolean isThreadStart(CFAEdge edge) {
    return isFunctionCall(edge, "pthread_create");
  }

  private boolean isThreadJoin(CFAEdge edge) {
    return isFunctionCall(edge, "pthread_join");
  }

  private boolean isFunctionCall(CFAEdge edge, String name) {
    return edge instanceof AStatementEdge statementEdge
        && statementEdge.getStatement() instanceof AFunctionCall functionCall
        && functionCall.getFunctionCallExpression().getFunctionNameExpression()
            instanceof AIdExpression functionName
        && name.equals(functionName.getName());
  }
}
