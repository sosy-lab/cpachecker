// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.util.dependencegraph.CallGraph;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.sdg.traversal.BackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.SdgVisitResult;
import org.sosy_lab.cpachecker.util.sdg.traversal.VisitOnceBackwardsSdgVisitor;

/** Class for computing summary edges and inserting them into a {@link SystemDependenceGraph}. */
public final class SummaryEdgeBuilder {

  /** Method for computing summary edges. */
  public enum Method {

    /** Find summary edges one by one. */
    SINGLE,

    /**
     * Find multiple summary edges at once. This may be useful for methods that have multiple
     * formal-in/out nodes.
     */
    BATCH;
  }

  private SummaryEdgeBuilder() {}

  /**
   * Compute summary edges reachable from a specified start procedure and insert them into a {@link
   * SystemDependenceGraph}.
   *
   * @param <P> the procedure type of the SDG
   * @param <N> the node type of the SDG
   * @param pBuilder the SDG builder used to insert summary edges
   * @param pCallGraph the call graph of the program
   * @param pStartProcedure the start procedure of the program (only summary edges reachable from
   *     this procedure are inserted)
   * @param pMethod the method used from computing summary edges
   */
  public static <P, N extends Node<P, ?, ?>> void insertSummaryEdges(
      SystemDependenceGraph.Builder<P, ?, ?, N> pBuilder,
      CallGraph<P> pCallGraph,
      P pStartProcedure,
      Method pMethod) {

    Multimap<P, N> formalOutNodesPerProcedure = ArrayListMultimap.create();
    for (N node : pBuilder.getNodes()) {
      if (node.getType() == NodeType.FORMAL_OUT) {
        formalOutNodesPerProcedure.put(node.getProcedure().orElseThrow(), node);
      }
    }

    List<N> orderedFormalOutNodes = new ArrayList<>();
    for (P procedure : pCallGraph.getPostOrder(pStartProcedure)) {
      orderedFormalOutNodes.addAll(formalOutNodesPerProcedure.get(procedure));
    }

    ImmutableSet<P> recursiveProcedures = pCallGraph.getRecursiveProcedures();
    int[] procedureIds = pBuilder.createIds(Node::getProcedure);

    SummaryEdgeFinder<N> summaryEdgeFinder;
    int batchSize;
    if (pMethod == Method.BATCH) {
      summaryEdgeFinder = new BatchSummaryEdgeFinder<>(pBuilder, procedureIds);
      batchSize = BatchSummaryEdgeFinder.MAX_BATCH_SIZE;
    } else {
      summaryEdgeFinder = new SingleSummaryEdgeFinder<>(pBuilder, procedureIds);
      batchSize = 1;
    }

    List<N> selectedFormalOutNodes = new ArrayList<>();
    // insert summary edges for all formal-out nodes
    for (int index = 0; index < orderedFormalOutNodes.size(); index++) {

      N node = orderedFormalOutNodes.get(index);
      int procedureId = procedureIds[node.getId()];
      selectedFormalOutNodes.add(node);

      // select up to batchSize formal-out nodes that are from a single procedure
      while (index + 1 < orderedFormalOutNodes.size()
          && selectedFormalOutNodes.size() < batchSize) {

        N nextNode = orderedFormalOutNodes.get(index + 1);

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

  @FunctionalInterface
  private interface SummaryEdgeConsumer<N extends Node<?, ?, ?>> {
    void accept(N pFormalInNode, N pFormalOutNode);
  }

  /**
   * A summary edge finder traverses a SDG and finds summary edges between formal-in and formal-out
   * nodes.
   *
   * <p>A list of formal-out nodes is specified for each run. The allowed list size may depend on
   * the implementation of summary edge finder. If a summary edge between a formal-in and formal-out
   * node is found by a summary edge finder, the {@link SummaryEdgeConsumer} specified for the run
   * is called.
   *
   * <p>Implementation must implement the run method.
   */
  private abstract static class SummaryEdgeFinder<N extends Node<?, ?, ?>> {

    private final SystemDependenceGraph.Builder<?, ?, ?, N> builder;
    private final int[] procedureIds;
    private final BitSet finished;
    private final List<N> reachedFormalInNodes;

    private SummaryEdgeFinder(
        SystemDependenceGraph.Builder<?, ?, ?, N> pBuilder, int[] pProcedureIds) {

      builder = pBuilder;
      procedureIds = pProcedureIds;
      finished = new BitSet(pBuilder.getNodeCount());
      reachedFormalInNodes = new ArrayList<>();
    }

    protected abstract void run(
        List<N> pFormalOutNode, boolean pRecursive, SummaryEdgeConsumer<N> pConsumer);

    protected void traverse(Collection<N> pStartNodes, BackwardsSdgVisitor<N> pVisitor) {
      builder.traverse(pStartNodes, pVisitor);
    }

    protected int getProcedureId(int pNodeTd) {
      return procedureIds[pNodeTd];
    }

    protected boolean isFormalOutFinished(int pNodeId) {
      return finished.get(pNodeId);
    }

    protected void setFormalOutFinished(int pNodeId) {
      finished.set(pNodeId);
    }

    protected List<N> getReachedFormalInNodes() {
      return reachedFormalInNodes;
    }

    protected void addReachedFormalInNode(N pFormalInNode) {
      reachedFormalInNodes.add(pFormalInNode);
    }

    protected void clearReachedFormalInNodes() {
      reachedFormalInNodes.clear();
    }
  }

  private static final class SingleSummaryEdgeFinder<N extends Node<?, ?, ?>>
      extends SummaryEdgeFinder<N> implements BackwardsSdgVisitor<N> {

    private final VisitOnceBackwardsSdgVisitor<N> visitor;

    private int procedureId;
    private boolean recursive;

    private SingleSummaryEdgeFinder(
        SystemDependenceGraph.Builder<?, ?, ?, N> pBuilder, int[] pProcedureIds) {
      super(pBuilder, pProcedureIds);

      visitor = new VisitOnceBackwardsSdgVisitor<>(this, pBuilder.getNodeCount());
    }

    @Override
    public void run(List<N> pFormalOutNodes, boolean pRecursive, SummaryEdgeConsumer<N> pConsumer) {

      for (N formalOutNode : pFormalOutNodes) {

        procedureId = getProcedureId(formalOutNode.getId());
        recursive = pRecursive;

        traverse(ImmutableList.of(formalOutNode), visitor);
        visitor.reset();

        for (N formalInNode : getReachedFormalInNodes()) {
          pConsumer.accept(formalInNode, formalOutNode);
        }

        setFormalOutFinished(formalOutNode.getId());
        clearReachedFormalInNodes();
      }
    }

    @Override
    public SdgVisitResult visitNode(N pNode) {

      if (pNode.getType() == NodeType.FORMAL_IN && getProcedureId(pNode.getId()) == procedureId) {
        addReachedFormalInNode(pNode);
      }

      return SdgVisitResult.CONTINUE;
    }

    @Override
    public SdgVisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {

      int predId = pPredecessor.getId();
      int succId = pSuccessor.getId();

      if (pPredecessor.getType() != NodeType.FORMAL_OUT) {

        int predProcedureId = getProcedureId(predId);
        int succProcedureId = getProcedureId(succId);

        if (predProcedureId != succProcedureId) {

          if (pType != EdgeType.PARAMETER_EDGE) {
            // don't leave procedure via non-parameter call edge
            return SdgVisitResult.SKIP;
          }

          if (succProcedureId == procedureId && !recursive) {
            // Procedure P contains the relevant formal-in/out edges for this traversal:
            // don't leave P via call edge if P does not recursively call itself
            return SdgVisitResult.SKIP;
          }
        }

      } else if (isFormalOutFinished(predId)) {
        // follow summary edges instead of return edges if they were already added
        return SdgVisitResult.SKIP;
      }

      return SdgVisitResult.CONTINUE;
    }
  }

  private static final class BatchSummaryEdgeFinder<N extends Node<?, ?, ?>>
      extends SummaryEdgeFinder<N> implements BackwardsSdgVisitor<N> {

    private static final int MAX_BATCH_SIZE = 64;
    private static final int EMPTY_STATE = 0;

    private int procedureId;
    private boolean recursive;

    // every element in states is used as a bitset and states[node.getId()] == state of node,
    // if a bit is set, the formal-out node corresponding to the bit is reachable from the node
    private final long[] states;
    // all states with index < statesDirtyMin have not set bits
    private int statesDirtyMin;
    // all states with index > statesDirtyMax have not set bits
    private int statesDirtyMax;

    private BatchSummaryEdgeFinder(
        SystemDependenceGraph.Builder<?, ?, ?, N> pBuilder, int[] pProcedureIds) {
      super(pBuilder, pProcedureIds);

      states = new long[pBuilder.getNodeCount()];
    }

    private void setStateDirty(int pNodeId) {
      statesDirtyMin = Math.min(statesDirtyMin, pNodeId);
      statesDirtyMax = Math.max(statesDirtyMax, pNodeId);
    }

    private boolean isFormalOutReachable(N pNode, int pFormalOutBit) {
      return (states[pNode.getId()] & (1L << pFormalOutBit)) != 0;
    }

    private void setFormalOutReachable(N pNode, int pFormalOutBit) {
      int nodeId = pNode.getId();
      states[nodeId] = 1L << pFormalOutBit;
      setStateDirty(nodeId);
    }

    @Override
    public void run(List<N> pFormalOutNodes, boolean pRecursive, SummaryEdgeConsumer<N> pConsumer) {

      Preconditions.checkArgument(!pFormalOutNodes.isEmpty(), "pFormalOutNodes must not be empty");
      Preconditions.checkArgument(
          pFormalOutNodes.size() <= MAX_BATCH_SIZE,
          "pFormalOutNodes must not contain more than MAX_BATCH_SIZE nodes");

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

      for (N formalInNode : getReachedFormalInNodes()) {
        for (int formalOutBit = 0; formalOutBit < pFormalOutNodes.size(); formalOutBit++) {
          if (isFormalOutReachable(formalInNode, formalOutBit)) {
            pConsumer.accept(formalInNode, pFormalOutNodes.get(formalOutBit));
          }
        }
      }

      Arrays.fill(states, statesDirtyMin, statesDirtyMax + 1, EMPTY_STATE);

      for (N formalOutNode : pFormalOutNodes) {
        setFormalOutFinished(formalOutNode.getId());
      }

      clearReachedFormalInNodes();
    }

    @Override
    public SdgVisitResult visitNode(N pNode) {
      return SdgVisitResult.CONTINUE;
    }

    @Override
    public SdgVisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {

      int predId = pPredecessor.getId();
      int succId = pSuccessor.getId();

      if (pPredecessor.getType() != NodeType.FORMAL_OUT) {

        int predProcedureId = getProcedureId(predId);
        int succProcedureId = getProcedureId(succId);

        if (predProcedureId != succProcedureId) {

          if (pType != EdgeType.PARAMETER_EDGE) {
            // don't leave procedure via non-parameter call edge
            return SdgVisitResult.SKIP;
          }

          if (succProcedureId == procedureId && !recursive) {
            // Procedure P contains the relevant formal-in/out edges for this traversal:
            // don't leave P via call edge if P does not recursively call itself
            return SdgVisitResult.SKIP;
          }
        }

        if (pPredecessor.getType() == NodeType.FORMAL_IN
            && states[predId] == EMPTY_STATE
            && predProcedureId == procedureId) {
          // relevant formal-in node reached for the first time
          addReachedFormalInNode(pPredecessor);
        }

      } else if (isFormalOutFinished(predId)) {
        // follow summary edges instead of return edges if they were already added
        return SdgVisitResult.SKIP;
      }

      // update state of predecessor, visit predecessor if state has changed
      long oldPredState = states[predId];
      long newPredState = (states[predId] |= states[succId]);

      if (oldPredState != newPredState) {
        setStateDirty(predId);
        return SdgVisitResult.CONTINUE;
      } else {
        return SdgVisitResult.SKIP;
      }
    }
  }
}
