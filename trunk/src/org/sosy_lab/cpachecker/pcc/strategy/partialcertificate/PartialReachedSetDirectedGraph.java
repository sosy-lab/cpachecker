// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

public class PartialReachedSetDirectedGraph implements Statistics {

  /** index of node is its position in <code>nodes</code> */
  private final AbstractState[] nodes;

  private final int numNodes;
  private final ImmutableList<ImmutableList<Integer>> adjacencyList;

  public PartialReachedSetDirectedGraph(final ARGState[] pNodes) {
    if (pNodes == null) {
      nodes = new AbstractState[0];
      numNodes = 0;
      adjacencyList = ImmutableList.of();
    } else {
      nodes = pNodes.clone();
      numNodes = nodes.length;
      adjacencyList = buildAdjacencyList(pNodes);
    }
  }

  private static ImmutableList<ImmutableList<Integer>> buildAdjacencyList(final ARGState[] pNodes) {
    List<List<Integer>> adjacencyList = new ArrayList<>(pNodes.length);
    for (@SuppressWarnings("unused") AbstractState node : pNodes) {
      adjacencyList.add(new ArrayList<Integer>());
    }

    SuccessorEdgeConstructor edgeConstructor = new SuccessorEdgeConstructor(pNodes, adjacencyList);
    for (ARGState node : pNodes) {
      edgeConstructor.setPredecessorBeforeARGPass(node);
      edgeConstructor.passARG(node);
    }

    List<ImmutableList<Integer>> newList = new ArrayList<>(adjacencyList.size());
    for (List<Integer> element : adjacencyList) {
      newList.add(ImmutableList.copyOf(element));
    }
    return ImmutableList.copyOf(newList);
  }

  public Set<Integer> getPredecessorsOf(int node) {
    Set<Integer> ret = new HashSet<>();
    for (int i = 0; i < getNumNodes(); i++) {
      if (i != node) {
        if (getAdjacencyList().get(i).contains(node)) {
          ret.add(i);
        }
      }
    }
    return ret;
  }

  public int getNumNodes() {
    return numNodes;
  }

  public List<AbstractState> getNodes() {
    return ImmutableList.copyOf(nodes);
  }

  public AbstractState getNode(int nodeIndex) {
    Preconditions.checkArgument(nodeIndex >= 0 && nodeIndex < numNodes);
    return nodes[nodeIndex];
  }

  public ImmutableList<ImmutableList<Integer>> getAdjacencyList() {
    return adjacencyList;
  }

  public AbstractState[] getSuccessorNodesOutsideSet(
      final Set<Integer> pNodeSetIndices, final boolean pAsARGState) {
    CollectingNodeVisitor visitor = new CollectingNodeVisitor(pAsARGState);
    visitOutsideSuccessors(pNodeSetIndices, visitor);

    return visitor.setRes.toArray(new AbstractState[0]);
  }

  public long getNumSuccessorNodesOutsideSet(final Set<Integer> pNodeSetIndices) {
    CountingNodeVisitor visitor = new CountingNodeVisitor();
    visitOutsideSuccessors(pNodeSetIndices, visitor);

    return visitor.numOutside;
  }

  public long getNumEdgesBetween(
      final Set<Integer> pSrcNodeSetIndices, final Set<Integer> pDstNodeSetIndices) {
    CountingNodeVisitor visitor = new CountingNodeVisitor();
    visitOutsideAdjacentNodes(pSrcNodeSetIndices, pDstNodeSetIndices, visitor);

    return visitor.numOutside;
  }

  public long getNumEdgesBetween(
      final Integer pSrcNodeIndex, final Set<Integer> pDstNodeSetIndices) {
    return getNumEdgesBetween(Sets.newHashSet(pSrcNodeIndex), pDstNodeSetIndices);
  }

  @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "rethrown as other type")
  public AbstractState[] getSetNodes(
      final Set<Integer> pNodeSetIndices, final boolean pAsARGState) {
    List<AbstractState> listRes = new ArrayList<>();

    try {
      for (Integer node : pNodeSetIndices) {

        if (pAsARGState) {
          listRes.add(nodes[node]);
        } else {
          listRes.add(((ARGState) nodes[node]).getWrappedState());
        }
      }
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      // TODO better style would be to check arguments first instead of catching exceptions
      // that could also come from other bugs.
      throw new IllegalArgumentException(
          "Wrong index set must not be null and all indices must be within [0;" + numNodes + "-1].",
          e);
    }
    return listRes.toArray(new AbstractState[0]);
  }

  @Override
  public void printStatistics(
      final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
    int edges = 0, maxin = 0, minin = Integer.MAX_VALUE, maxout = 0, minout = Integer.MAX_VALUE;
    double avgin = 0, avgout = 0;
    // store distribution of nodes over there degrees
    final int MAX_DEG = 10; // All nodes with same or higher degrees are counted as MAX_DEG-nodes
    int[] inDistribution = new int[MAX_DEG];
    double[] normalIn =
        new double[MAX_DEG]; // normalized Distribution, i.e. divide any element by #nodes
    int[] outDistribution = new int[MAX_DEG];
    double[] normalOut = new double[MAX_DEG];

    int successorSize;

    int[] indegrees = new int[nodes.length];

    for (ImmutableList<Integer> successors : adjacencyList) {
      successorSize = successors.size();
      edges += successors.size();
      if (successorSize >= MAX_DEG) { // node with out degree higher than MAX_DEG-1
        outDistribution[MAX_DEG - 1]++;
      } else { // successorSize in [0:MAX_DEG]
        outDistribution[successorSize]++;
      }
      maxout = Math.max(maxout, successorSize);
      minout = Math.min(minout, successorSize);
      avgout += successorSize;
      for (Integer succ : successors) {
        indegrees[succ] = indegrees[succ] + 1;
      }
    }
    if (nodes.length > 0) {
      for (int a = 0; a < outDistribution.length; a++) {
        normalOut[a] = (double) outDistribution[a] / nodes.length;
      }

      Arrays.sort(indegrees);
      minin = indegrees[0];
      maxin = indegrees[indegrees.length - 1];
      for (int indegree : indegrees) {
        avgin += indegree;
        if (indegree >= MAX_DEG) { // node with in degree higher than MAX_DEG-1
          inDistribution[MAX_DEG - 1]++;
        } else { // predecessorSize in [0:MAX_DEG]
          inDistribution[indegree]++;
        }
      }
      avgin = avgin / nodes.length;
      avgout = avgout / nodes.length;
      for (int a = 0; a < inDistribution.length; a++) {
        normalIn[a] = (double) inDistribution[a] / nodes.length;
      }
    } else {
      minin = 0;
      minout = 0;
    }

    pOut.println("#nodes:         " + nodes.length);
    pOut.println("#edges:         " + edges);
    pOut.println("max indegree:   " + maxin);
    pOut.println("min indegree:   " + minin);
    pOut.println("in distribution:   " + Arrays.toString(inDistribution));
    pOut.println("relative in distr:   " + Arrays.toString(normalIn));
    pOut.printf("avg. indegree:  %.2f%n", avgin);
    pOut.println("max outdegree:  " + maxout);
    pOut.println("min outdegree:  " + minout);
    pOut.println("out distribution:   " + Arrays.toString(outDistribution));
    pOut.println("relative out distr:   " + Arrays.toString(normalOut));
    pOut.printf("avg. outdegree: %.2f%n", avgout);
  }

  @Override
  public @Nullable String getName() {
    return null;
  }

  private void visitOutsideSuccessorsOf(
      final int pPredecessor, final NodeVisitor pVisitor, final Predicate<Integer> pMustVisit) {
    for (Integer successor : adjacencyList.get(pPredecessor)) {
      if (pMustVisit.apply(successor)) {
        pVisitor.visit(successor);
      }
    }
  }

  @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "rethrown as other type")
  private void visitOutsideSuccessors(final Set<Integer> pNodeSet, final NodeVisitor pVisitor) {
    try {
      Predicate<Integer> isOutsideSet = pNode -> !pNodeSet.contains(pNode);
      for (int predecessor : pNodeSet) {
        visitOutsideSuccessorsOf(predecessor, pVisitor, isOutsideSet);
      }
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      // TODO better style would be to check arguments first instead of catching exceptions
      // that could also come from other bugs.
      throw new IllegalArgumentException(
          "Wrong index set must not be null and all indices be within [0;" + numNodes + "-1].", e);
    }
  }

  @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "rethrown as other type")
  private void visitOutsideAdjacentNodes(
      final Set<Integer> pSrcNodeSetIndices,
      final Set<Integer> pDstNodeSetIndices,
      final NodeVisitor pVisitor) {
    try {
      visitSuccessorsInOtherSet(pSrcNodeSetIndices, pDstNodeSetIndices, pVisitor);
      visitSuccessorsInOtherSet(pDstNodeSetIndices, pSrcNodeSetIndices, pVisitor);
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      // TODO better style would be to check arguments first instead of catching exceptions
      // that could also come from other bugs.
      throw new IllegalArgumentException(
          "Wrong index set must not be null and all indices be within [0;" + numNodes + "-1].", e);
    }
  }

  private void visitSuccessorsInOtherSet(
      final Set<Integer> pNodeSet, final Set<Integer> pOtherNodeSet, final NodeVisitor pVisitor) {
    Predicate<Integer> isInOtherSet = pOtherNodeSet::contains;
    for (int predecessor : pNodeSet) {
      visitOutsideSuccessorsOf(predecessor, pVisitor, isInOtherSet);
    }
  }

  private interface NodeVisitor {

    void visit(int pSuccessor);
  }

  private class CollectingNodeVisitor implements NodeVisitor {

    private final Set<AbstractState> setRes = new HashSet<>();
    private final boolean collectAsARGState;

    public CollectingNodeVisitor(final boolean pCollectAsARGState) {
      collectAsARGState = pCollectAsARGState;
    }

    @Override
    public void visit(int pSuccessor) {
      if (collectAsARGState) {
        setRes.add(nodes[pSuccessor]);
      } else {
        setRes.add(((ARGState) nodes[pSuccessor]).getWrappedState());
      }
    }
  }

  private static class CountingNodeVisitor implements NodeVisitor {

    private long numOutside = 0;

    @Override
    public void visit(int pSuccessor) {
      numOutside++;
    }
  }

  private static class SuccessorEdgeConstructor extends AbstractARGPass {

    private ARGState predecessor;
    private int indexPredecessor;
    private final Map<AbstractState, Integer> nodeToIndex;
    private final List<List<Integer>> changeableAdjacencyList;
    private final Set<Pair<Integer, Integer>> knownEdges;

    public SuccessorEdgeConstructor(ARGState[] pNodes, List<List<Integer>> pAdjacencyList) {
      super(false);
      nodeToIndex = new HashMap<>();
      for (int i = 0; i < pNodes.length; i++) {
        nodeToIndex.put(pNodes[i], i);
      }
      changeableAdjacencyList = pAdjacencyList;
      knownEdges = Sets.newHashSetWithExpectedSize(pNodes.length);
    }

    public void setPredecessorBeforeARGPass(ARGState pNewPredecessor) {
      predecessor = pNewPredecessor;
      indexPredecessor = nodeToIndex.get(predecessor);
    }

    @Override
    public void visitARGNode(ARGState pNode) {
      if (stopPathDiscovery(pNode)) {
        while (pNode.isCovered()) {
          pNode = pNode.getCoveringState();
        }
        int indexSuccessor = nodeToIndex.get(pNode);
        Pair<Integer, Integer> edge = Pair.of(indexPredecessor, indexSuccessor);
        if (knownEdges.add(edge)) {
          changeableAdjacencyList.get(indexPredecessor).add(indexSuccessor);
        }
      }
    }

    @Override
    public boolean stopPathDiscovery(ARGState pNode) {
      return !Objects.equals(pNode, predecessor)
          && (nodeToIndex.containsKey(pNode) || pNode.isCovered());
    }
  }
}
