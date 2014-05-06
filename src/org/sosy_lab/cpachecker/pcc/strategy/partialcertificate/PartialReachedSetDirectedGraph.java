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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.collect.ImmutableList;

public class PartialReachedSetDirectedGraph {

  /* index of node is its position in <code>nodes</code>*/
  private final AbstractState[] nodes;
  private final int numNodes;
  private final int[][] adjacencyMatrix;
  private final ImmutableList<ImmutableList<Integer>> adjacencyList;

  public PartialReachedSetDirectedGraph(final ARGState[] pNodes) {
    List<List<Integer>> adjacencyList;
    if (pNodes == null) {
      nodes = new AbstractState[0];
      numNodes = 0;
      adjacencyMatrix = new int[nodes.length][nodes.length];
      adjacencyList = new ArrayList<>(0);
    } else {
      nodes = pNodes;
      numNodes = nodes.length;
      adjacencyMatrix = new int[nodes.length][nodes.length];
      adjacencyList = new ArrayList<>(nodes.length);
      for(int i=0;i<nodes.length;i++){
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

  public int getNumNodes(){
    return numNodes;
  }
  public List<AbstractState> getNodes() {
    return ImmutableList.copyOf(nodes);
  }

  public int[][] getAdjacencyMatrix() {
    int[][] returnMatrix = new int[nodes.length][nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      for (int j = 0; j < nodes.length; j++) {
        returnMatrix[i][j] = adjacencyMatrix[i][j];
      }
    }
    return returnMatrix;
  }

  public ImmutableList<ImmutableList<Integer>> getAdjacencyList() {
    return adjacencyList;
  }

  public AbstractState[] getAdjacentNodesOutsideSet(final Set<Integer> pNodeSetIndices, final boolean pAsARGState) {
    CollectingOutsideSuccessorVisitor visitor = new CollectingOutsideSuccessorVisitor(pAsARGState);
    visitOutsideSuccessors(pNodeSetIndices, visitor);

    return visitor.listRes.toArray(new AbstractState[visitor.listRes.size()]);
  }

  public long getNumAdjacentNodesOutsideSet(final Set<Integer> pNodeSetIndices) {
    CountingOutsideSuccessorVisitor visitor = new CountingOutsideSuccessorVisitor();
    visitOutsideSuccessors(pNodeSetIndices, visitor);

    return visitor.numOutside;
  }

  public AbstractState[] getSetNodes(final Set<Integer> pNodeSetIndices, final boolean pAsARGState){
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

  private void visitOutsideSuccessors(final Set<Integer> pNodeSetIndices, final OutsideSuccessorVisitor pVisitor) {
    try {
      List<Integer> successors;
      for (Integer predecessor : pNodeSetIndices) {
        successors = adjacencyList.get(predecessor);
        for (int successor : successors) {
          if (!pNodeSetIndices.contains(successor)) {
            pVisitor.visit(successor);
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      throw new IllegalArgumentException("Wrong index set must not be null and all indices be within [0;" + numNodes
          + "-1].");
    }
  }

  private interface OutsideSuccessorVisitor {

    void visit(int pSuccessor);

  }

  private class CollectingOutsideSuccessorVisitor implements OutsideSuccessorVisitor {

    private final List<AbstractState> listRes = new ArrayList<>();
    private final boolean collectAsARGState;

    public CollectingOutsideSuccessorVisitor(final boolean pCollectAsARGState) {
      collectAsARGState = pCollectAsARGState;
    }

    @Override
    public void visit(int pSuccessor) {
      if (collectAsARGState) {
        listRes.add(nodes[pSuccessor]);
      } else {
        listRes.add(((ARGState) nodes[pSuccessor]).getWrappedState());
      }
    }
  }

  private static class CountingOutsideSuccessorVisitor implements OutsideSuccessorVisitor {

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

    public void setPredecessorBeforeARGPass(ARGState pNewPredecessor){
      predecessor = pNewPredecessor;
      indexPredecessor = nodeToIndex.get(predecessor);
    }

    @Override
    public void visitARGNode(ARGState pNode) {
      if(stopPathDiscovery(pNode)){
         int indexSuccessor = nodeToIndex.get(pNode);
         adjacencyMatrix[indexPredecessor][indexSuccessor] = 1;
         changeableAdjacencyList.get(indexPredecessor).add(indexSuccessor);
      }
    }

    @Override
    public boolean stopPathDiscovery(ARGState pNode) {
      return pNode!=predecessor && nodeToIndex.containsKey(pNode);
    }

  }

}
