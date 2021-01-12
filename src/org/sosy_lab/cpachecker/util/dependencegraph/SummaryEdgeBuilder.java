// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.BackwardsVisitOnceVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.BackwardsVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;

final class SummaryEdgeBuilder {

  private SummaryEdgeBuilder() {}

  public static <P, T, V> void insertSummaryEdges(
      SystemDependenceGraph.Builder<P, T, V> pBuilder,
      CallGraph<P> pCallGraph,
      P pStartProcedure,
      Method pMethod) {

    Multimap<P, Node<P, T, V>> formalOutNodesPerProcedure = ArrayListMultimap.create();
    for (Node<P, T, V> node : pBuilder.getNodes()) {
      if (node.getType() == NodeType.FORMAL_OUT) {
        formalOutNodesPerProcedure.put(node.getProcedure().orElseThrow(), node);
      }
    }

    List<Node<P, T, V>> orderedFormalOutNodes = new ArrayList<>();
    for (P procedure : pCallGraph.getPostOrder(pStartProcedure)) {
      orderedFormalOutNodes.addAll(formalOutNodesPerProcedure.get(procedure));
    }

    ImmutableSet<P> recursiveProcedures = pCallGraph.getRecursiveProcedures();
    int[] procedureIds = pBuilder.createIds(Node::getProcedure);

    SummaryEdgeFinder<P, T, V> summaryEdgeFinder;
    int batchSize;
    if (pMethod == Method.BATCH) {
      summaryEdgeFinder = new BatchSummaryEdgeFinder<>(pBuilder, procedureIds);
      batchSize = BatchSummaryEdgeFinder.MAX_BATCH_SIZE;
    } else {
      summaryEdgeFinder = new SingleSummaryEdgeFinder<>(pBuilder, procedureIds);
      batchSize = 1;
    }

    List<Node<P, T, V>> selectedFormalOutNodes = new ArrayList<>();

    for (int index = 0; index < orderedFormalOutNodes.size(); index++) {

      Node<P, T, V> node = orderedFormalOutNodes.get(index);
      int procedureId = procedureIds[node.getId()];
      selectedFormalOutNodes.add(node);

      while (index + 1 < orderedFormalOutNodes.size()
          && selectedFormalOutNodes.size() < batchSize) {

        Node<P, T, V> nextNode = orderedFormalOutNodes.get(index + 1);

        if (procedureIds[nextNode.getId()] != procedureId) {
          break;
        }

        selectedFormalOutNodes.add(nextNode);
        index++;
      }

      boolean recursive = recursiveProcedures.contains(node.getProcedure().orElseThrow());
      summaryEdgeFinder.run(selectedFormalOutNodes, recursive, pBuilder::insertActualSummaryEdges);
      selectedFormalOutNodes.clear();
    }
  }

  public enum Method {
    SINGLE,
    BATCH;
  }

  @FunctionalInterface
  private interface SummaryEdgeConsumer<P, T, V> {
    void accept(Node<P, T, V> pFormalInNode, Node<P, T, V> pFormalOutNode);
  }

  private abstract static class SummaryEdgeFinder<P, T, V> {

    private final SystemDependenceGraph.Builder<P, T, V> builder;
    private final int[] procedureIds;
    private final BitSet finished;
    private final List<Node<P, T, V>> reachedFormalInNodes;

    private SummaryEdgeFinder(
        SystemDependenceGraph.Builder<P, T, V> pBuilder, int[] pProcedureIds) {

      builder = pBuilder;
      procedureIds = pProcedureIds;
      finished = new BitSet(pBuilder.getNodeCount());
      reachedFormalInNodes = new ArrayList<>();
    }

    protected abstract void run(
        List<Node<P, T, V>> pFormalOutNode,
        boolean pRecursive,
        SummaryEdgeConsumer<P, T, V> pConsumer);

    protected void traverse(
        Collection<Node<P, T, V>> pStartNodes, BackwardsVisitor<P, T, V> pVisitor) {
      builder.traverse(pStartNodes, pVisitor);
    }

    protected int getProcedureId(int pNodeTd) {
      return procedureIds[pNodeTd];
    }

    protected boolean isFormalOutFinished(int pNodeId) {
      // FIXME: When 'true' is returned, previously added summary edges are followed instead of
      // traversing the procedure again. This leads to different results, which should not happen!
      // return finished.get(pNodeId);
      return pNodeId < 0;
    }

    protected void setFormalOutFinished(int pNodeId) {
      finished.set(pNodeId);
    }

    protected List<Node<P, T, V>> getReachedFormalInNodes() {
      return reachedFormalInNodes;
    }

    protected void addReachedFormalInNode(Node<P, T, V> pFormalInNode) {
      reachedFormalInNodes.add(pFormalInNode);
    }

    protected void clearReachedFormalInNodes() {
      reachedFormalInNodes.clear();
    }
  }

  private static final class SingleSummaryEdgeFinder<P, T, V> extends SummaryEdgeFinder<P, T, V>
      implements BackwardsVisitor<P, T, V> {

    private final BackwardsVisitOnceVisitor<P, T, V> visitor;

    private int procedureId;
    private boolean recursive;

    private SingleSummaryEdgeFinder(
        SystemDependenceGraph.Builder<P, T, V> pBuilder, int[] pProcedureIds) {
      super(pBuilder, pProcedureIds);

      visitor = new BackwardsVisitOnceVisitor<>(this, pBuilder.getNodeCount());
    }

    @Override
    public void run(
        List<Node<P, T, V>> pFormalOutNodes,
        boolean pRecursive,
        SummaryEdgeConsumer<P, T, V> pConsumer) {

      for (Node<P, T, V> formalOutNode : pFormalOutNodes) {

        procedureId = getProcedureId(formalOutNode.getId());
        recursive = pRecursive;

        traverse(ImmutableList.of(formalOutNode), visitor);
        visitor.reset();

        for (Node<P, T, V> formalInNode : getReachedFormalInNodes()) {
          pConsumer.accept(formalInNode, formalOutNode);
        }

        setFormalOutFinished(formalOutNode.getId());
        clearReachedFormalInNodes();
      }
    }

    @Override
    public VisitResult visitNode(Node<P, T, V> pNode) {

      if (pNode.getType() == NodeType.FORMAL_IN && getProcedureId(pNode.getId()) == procedureId) {
        addReachedFormalInNode(pNode);
      }

      return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {

      int predId = pPredecessor.getId();
      int succId = pSuccessor.getId();

      if (pPredecessor.getType() != NodeType.FORMAL_OUT) {

        int predProcedureId = getProcedureId(predId);
        int succProcedureId = getProcedureId(succId);

        if (predProcedureId != succProcedureId && succProcedureId == procedureId && !recursive) {
          // don't leave procedure via call edge if procedure does not recursively call itself
          return VisitResult.SKIP;
        }

      } else if (isFormalOutFinished(predId)) {
        // follow summary edges instead of return edges if they were already added
        return VisitResult.SKIP;
      }

      return VisitResult.CONTINUE;
    }
  }

  private static final class BatchSummaryEdgeFinder<P, T, V> extends SummaryEdgeFinder<P, T, V>
      implements BackwardsVisitor<P, T, V> {

    private static final int MAX_BATCH_SIZE = 64;
    private static final int EMPTY_STATE = 0;

    private int procedureId;
    private boolean recursive;

    private final long[] states;
    private int statesDirtyMin;
    private int statesDirtyMax;

    private BatchSummaryEdgeFinder(
        SystemDependenceGraph.Builder<P, T, V> pBuilder, int[] pProcedureIds) {
      super(pBuilder, pProcedureIds);

      states = new long[pBuilder.getNodeCount()];
    }

    private void setStateDirty(int pNodeId) {
      statesDirtyMin = Math.min(statesDirtyMin, pNodeId);
      statesDirtyMax = Math.max(statesDirtyMax, pNodeId);
    }

    private boolean isFormalOutReachable(Node<P, T, V> pNode, int pFormalOutBit) {
      return (states[pNode.getId()] & (1L << pFormalOutBit)) != 0;
    }

    private void setFormalOutReachable(Node<P, T, V> pNode, int pFormalOutBit) {
      int nodeId = pNode.getId();
      states[nodeId] = 1L << pFormalOutBit;
      setStateDirty(nodeId);
    }

    @Override
    public void run(
        List<Node<P, T, V>> pFormalOutNodes,
        boolean pRecursive,
        SummaryEdgeConsumer<P, T, V> pConsumer) {

      assert !pFormalOutNodes.isEmpty();
      assert pFormalOutNodes.size() <= MAX_BATCH_SIZE;

      procedureId = getProcedureId(pFormalOutNodes.get(0).getId());
      recursive = pRecursive;
      statesDirtyMin = states.length - 1;
      statesDirtyMax = 0;

      for (int formalOutBit = 0; formalOutBit < pFormalOutNodes.size(); formalOutBit++) {

        assert pFormalOutNodes.get(formalOutBit).getType() == NodeType.FORMAL_OUT;
        assert getProcedureId(pFormalOutNodes.get(formalOutBit).getId()) == procedureId;

        setFormalOutReachable(pFormalOutNodes.get(formalOutBit), formalOutBit);
      }

      traverse(pFormalOutNodes, this);

      for (Node<P, T, V> formalInNode : getReachedFormalInNodes()) {
        for (int formalOutBit = 0; formalOutBit < pFormalOutNodes.size(); formalOutBit++) {
          if (isFormalOutReachable(formalInNode, formalOutBit)) {
            pConsumer.accept(formalInNode, pFormalOutNodes.get(formalOutBit));
          }
        }
      }

      Arrays.fill(states, statesDirtyMin, statesDirtyMax + 1, EMPTY_STATE);

      for (Node<P, T, V> formalOutNode : pFormalOutNodes) {
        setFormalOutFinished(formalOutNode.getId());
      }

      clearReachedFormalInNodes();
    }

    @Override
    public VisitResult visitNode(Node<P, T, V> pNode) {
      return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {

      int predId = pPredecessor.getId();
      int succId = pSuccessor.getId();

      if (pPredecessor.getType() != NodeType.FORMAL_OUT) {

        int predProcedureId = getProcedureId(predId);
        int succProcedureId = getProcedureId(succId);

        if (predProcedureId != succProcedureId && succProcedureId == procedureId && !recursive) {
          // don't leave procedure via call edge if procedure does not recursively call itself
          return VisitResult.SKIP;
        }

        if (pPredecessor.getType() == NodeType.FORMAL_IN
            && states[predId] == EMPTY_STATE
            && predProcedureId == procedureId) {
          // relevant formal-in node reached for the first time
          addReachedFormalInNode(pPredecessor);
        }

      } else if (isFormalOutFinished(predId)) {
        // follow summary edges instead of return edges if they were already added
        return VisitResult.SKIP;
      }

      // update state of predecessor, visit predecessor if state has changed
      long oldPredState = states[predId];
      long newPredState = (states[predId] |= states[succId]);

      if (oldPredState != newPredState) {
        setStateDirty(predId);
        return VisitResult.CONTINUE;
      } else {
        return VisitResult.SKIP;
      }
    }
  }
}
