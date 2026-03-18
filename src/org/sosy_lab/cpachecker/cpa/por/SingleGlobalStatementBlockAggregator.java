// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.composite.StraightLineBlockAggregator;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;

class SingleGlobalStatementBlockAggregator extends StraightLineBlockAggregator {

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  private final Collection<CFANode> initializationPhaseNodes;

  SingleGlobalStatementBlockAggregator(CFA pCfa) {
    super(pCfa);
    initializationPhaseNodes = getInitializationPhaseNodes(pCfa);
  }

  @Override
  public boolean isValidMultiEdgeComponent(CFANode startNode, CFAEdge edge) {
    if (!super.isValidMultiEdgeComponent(startNode, edge)) {
      return false;
    }
    if (startNode.equals(edge.getPredecessor())) {
      return true;
    }
    if (isThreadJoin(edge)) {
      return false;
    }
    if (initializationPhaseNodes.contains(edge.getPredecessor())) {
      return true;
    }

    boolean anyGlobalStatements = false;
    CFAEdge currentEdge = edge;
    while (true) {
      var accesses = memoryAccessExtractor.extract(currentEdge);
      if (!accesses.getUses().isEmpty() ||
          !accesses.getDefs().isEmpty() ||
          !accesses.getPointeeDefs().isEmpty() ||
          !accesses.getPointeeUses().isEmpty()) {
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
      assert predecessor.getEnteringEdges().size() == 1
          : "Multi-edge component must be a straight line";
      currentEdge = predecessor.getEnteringEdges().iterator().next();
    }
  }

  private Collection<CFANode> getInitializationPhaseNodes(CFA pCFA) {
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

    return nodesBeforeAnyThreadStart;
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
        && functionCall.getFunctionCallExpression()
        .getFunctionNameExpression() instanceof AIdExpression functionName
        && name.equals(functionName.getName());
  }
}
