// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.BackwardsVisitOnceVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.BackwardsVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;

final class SummaryEdgeBuilder {

  private SummaryEdgeBuilder() {}

  public static <P, T, V> void insertSummaryEdges(
      SystemDependenceGraph.Builder<P, T, V> pBuilder, Collection<P> pProcedures) {

    List<Node<P, T, V>> formalOutNodes = new ArrayList<>();
    for (Node<P, T, V> node : pBuilder.getNodes()) {
      if (node.getType() == NodeType.FORMAL_OUT) {
        Optional<P> procedure = node.getProcedure();
        if (procedure.isPresent() && pProcedures.contains(procedure.orElseThrow())) {
          formalOutNodes.add(node);
        }
      }
    }

    SimpleSummaryEdgeFinder<P, T, V> summaryFinder = new SimpleSummaryEdgeFinder<>(pBuilder);
    for (Node<P, T, V> formalOutNode : formalOutNodes) {

      Collection<Node<P, T, V>> formalInNodes = summaryFinder.run(formalOutNode);
      for (Node<P, T, V> formalInNode : formalInNodes) {
        pBuilder.insertActualSummaryEdges(formalInNode, formalOutNode);
      }
    }
  }

  private static final class SimpleSummaryEdgeFinder<P, T, V> implements BackwardsVisitor<P, T, V> {

    private final SystemDependenceGraph.Builder<P, T, V> builder;
    private final BackwardsVisitOnceVisitor<P, T, V> visitor;

    private final BitSet finishedFormalOutNodes;
    private final int[] procedureIds;

    private int currentProcedureId;
    private boolean currentRecursive;
    private final Set<Node<P, T, V>> currentRelevantFormalInNodes;

    private SimpleSummaryEdgeFinder(SystemDependenceGraph.Builder<P, T, V> pBuilder) {

      builder = pBuilder;
      visitor = new BackwardsVisitOnceVisitor<>(this, pBuilder.getNodeCount());

      finishedFormalOutNodes = new BitSet(pBuilder.getNodeCount());
      procedureIds = pBuilder.createIds(Node::getProcedure);
      ;

      currentRelevantFormalInNodes = new HashSet<>();
      currentProcedureId = -1;
      currentRecursive = false;
    }

    private Collection<Node<P, T, V>> run(Node<P, T, V> pFormalOutNode) {

      currentProcedureId = procedureIds[pFormalOutNode.getId()];
      currentRecursive = false;
      currentRelevantFormalInNodes.clear();

      builder.traverse(ImmutableList.of(pFormalOutNode), visitor);
      visitor.reset();

      if (currentRecursive) {
        builder.traverse(ImmutableList.of(pFormalOutNode), visitor);
        visitor.reset();
      }

      finishedFormalOutNodes.set(pFormalOutNode.getId());

      return currentRelevantFormalInNodes;
    }

    @Override
    public VisitResult visitNode(Node<P, T, V> pNode) {

      if (pNode.getType() == NodeType.FORMAL_IN
          && procedureIds[pNode.getId()] == currentProcedureId) {
        currentRelevantFormalInNodes.add(pNode);
      }

      return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {

      if (pPredecessor.getType() == NodeType.FORMAL_OUT) {

        if (procedureIds[pPredecessor.getId()] == currentProcedureId && !currentRecursive) {
          currentRecursive = true;
          return VisitResult.TERMINATE;
        }

        if (finishedFormalOutNodes.get(pPredecessor.getId())) {
          return VisitResult.SKIP;
        }
      }

      int predecessorProcedureId = procedureIds[pPredecessor.getId()];
      int successorProcedureId = procedureIds[pSuccessor.getId()];

      if (predecessorProcedureId != successorProcedureId
          && successorProcedureId == currentProcedureId
          && !currentRecursive) {
        return VisitResult.SKIP;
      }

      return VisitResult.CONTINUE;
    }
  }
}
