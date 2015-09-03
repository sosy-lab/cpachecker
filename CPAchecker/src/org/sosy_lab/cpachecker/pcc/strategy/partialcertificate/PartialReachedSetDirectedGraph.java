/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

public class PartialReachedSetDirectedGraph {

  /* index of node is its position in <code>nodes</code>*/
  private final AbstractState[] nodes;
  private final int numNodes;
  private final ImmutableList<ImmutableList<Integer>> adjacencyList;

  public PartialReachedSetDirectedGraph(final ARGState[] pNodes) {
    List<List<Integer>> adjacencyList;
    if (pNodes == null) {
      nodes = new AbstractState[0];
      numNodes = 0;
      adjacencyList = new ArrayList<>(0);
    } else {
      nodes = Arrays.copyOf(pNodes, pNodes.length);
      numNodes = nodes.length;
      adjacencyList = new ArrayList<>(nodes.length);
      for (@SuppressWarnings("unused") AbstractState node : nodes) {
        adjacencyList.add(new ArrayList<Integer>());
      }

      SuccessorEdgeConstructor edgeConstructor = new SuccessorEdgeConstructor(adjacencyList);
      for (ARGState node : pNodes) {
        edgeConstructor.setPredecessorBeforeARGPass(node);
        edgeConstructor.passARG(node);
      }
    }

    List<ImmutableList<Integer>> newList = new ArrayList<>(adjacencyList.size());
    for (int i = 0; i < adjacencyList.size(); i++) {
      newList.add(ImmutableList.copyOf(adjacencyList.get(i)));
    }
    this.adjacencyList = ImmutableList.copyOf(newList);
  }

  public Set<Integer> getPredecessorsOf(int node) {
    Set<Integer> ret = new HashSet<>();
    for(int i = 0; i < getNumNodes(); i++) {
      if(i != node) {
        if(getAdjacencyList().get(i).contains(node)) {
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


  public ImmutableList<ImmutableList<Integer>> getAdjacencyList() {
    return adjacencyList;
  }

  public AbstractState[] getSuccessorNodesOutsideSet(final Set<Integer> pNodeSetIndices, final boolean pAsARGState) {
    CollectingNodeVisitor visitor = new CollectingNodeVisitor(pAsARGState);
    visitOutsideSuccessors(pNodeSetIndices, visitor);

    return visitor.setRes.toArray(new AbstractState[visitor.setRes.size()]);
  }

  public long getNumSuccessorNodesOutsideSet(final Set<Integer> pNodeSetIndices) {
    CountingNodeVisitor visitor = new CountingNodeVisitor();
    visitOutsideSuccessors(pNodeSetIndices, visitor);

    return visitor.numOutside;
  }

  public long getNumEdgesBetween(final Set<Integer> pSrcNodeSetIndices, final Set<Integer> pDstNodeSetIndices){
    CountingNodeVisitor visitor = new CountingNodeVisitor();
    visitOutsideAdjacentNodes(pSrcNodeSetIndices, pDstNodeSetIndices, visitor);

    return visitor.numOutside;
  }

  public long getNumEdgesBetween(final Integer pSrcNodeIndex, final Set<Integer> pDstNodeSetIndices) {
    return getNumEdgesBetween(Sets.newHashSet(pSrcNodeIndex), pDstNodeSetIndices);
  }

  public AbstractState[] getSetNodes(final Set<Integer> pNodeSetIndices, final boolean pAsARGState) {
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
      throw new IllegalArgumentException("Wrong index set must not be null and all indices must be within [0;" + numNodes + "-1].");
    }
     return listRes.toArray(new AbstractState[listRes.size()]);
  }

  private void visitOutsideSuccessorsOf(final int pPredecessor, final NodeVisitor pVisitor,
      final Predicate<Integer> pMustVisit) {
    for (Integer successor : adjacencyList.get(pPredecessor)) {
      if (pMustVisit.apply(successor)) {
        pVisitor.visit(successor);
      }
    }
  }

  private void visitOutsideSuccessors(final Set<Integer> pNodeSet, final NodeVisitor pVisitor) {
    try {
      Predicate<Integer> isOutsideSet = new Predicate<Integer>() {

        @Override
        public boolean apply(@Nullable Integer pNode) {
          return !pNodeSet.contains(pNode);
        }
      };
      for (int predecessor : pNodeSet) {
        visitOutsideSuccessorsOf(predecessor, pVisitor, isOutsideSet);
      }
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      throw new IllegalArgumentException("Wrong index set must not be null and all indices be within [0;" + numNodes
          + "-1].");
    }
  }

  private void visitOutsideAdjacentNodes(final Set<Integer> pSrcNodeSetIndices, final Set<Integer> pDstNodeSetIndices,
      final NodeVisitor pVisitor) {
    try {
      visitSuccessorsInOtherSet(pSrcNodeSetIndices, pDstNodeSetIndices, pVisitor);
      visitSuccessorsInOtherSet(pDstNodeSetIndices, pSrcNodeSetIndices, pVisitor);
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      throw new IllegalArgumentException("Wrong index set must not be null and all indices be within [0;" + numNodes
          + "-1].");
    }
  }

  private void visitSuccessorsInOtherSet(final Set<Integer> pNodeSet, final Set<Integer> pOtherNodeSet,
      final NodeVisitor pVisitor) {
    Predicate<Integer> isInOtherSet = new Predicate<Integer>() {

      @Override
      public boolean apply(@Nullable Integer pNode) {
        return pOtherNodeSet.contains(pNode);
      }
    };
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


  private class SuccessorEdgeConstructor extends AbstractARGPass{

    private ARGState predecessor;
    private int indexPredecessor;
    private final HashMap<AbstractState, Integer> nodeToIndex;
    private final List<List<Integer>> changeableAdjacencyList;

    public SuccessorEdgeConstructor(List<List<Integer>> pAdjacencyList) {
      super(false);
      nodeToIndex = new HashMap<>();
      for (int i = 0; i < nodes.length; i++) {
        nodeToIndex.put(nodes[i], i);
      }
      changeableAdjacencyList = pAdjacencyList;
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
        changeableAdjacencyList.get(indexPredecessor).add(indexSuccessor);
      }
    }

    @Override
    public boolean stopPathDiscovery(ARGState pNode) {
      return pNode != predecessor && (nodeToIndex.containsKey(pNode) || pNode.isCovered());
    }

  }

}
