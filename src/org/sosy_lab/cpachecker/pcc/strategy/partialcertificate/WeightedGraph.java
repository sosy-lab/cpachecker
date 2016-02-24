/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

/**
 * Store and access a weighted graph structure
 */
public class WeightedGraph {

  private final int numNodes;
  private final WeightedNode[] nodes;
  /**
   * store for each node its out- and incoming edges (each edge is stored twice)
   */
  private final Map<Integer, Set<WeightedEdge>> outgoingEdges;
  private final Map<Integer, Set<WeightedEdge>> incomingEdges;


  public WeightedGraph(PartialReachedSetDirectedGraph pGraph) {
    int weight = 1;
    numNodes = pGraph.getNumNodes();
    nodes = new WeightedNode[numNodes];
    outgoingEdges = new HashMap<>(numNodes);
    incomingEdges = new HashMap<>(numNodes);

    ImmutableList<ImmutableList<Integer>> adjacencyList = pGraph.getAdjacencyList();

    for (int actualNode = 0; actualNode < numNodes; actualNode++) {
      WeightedNode start = new WeightedNode(actualNode, weight); //start node of newly found edges
      for (Integer successorNode : adjacencyList.get(actualNode)) { //iterate over successors of actual node
        WeightedNode end = new WeightedNode(successorNode, weight);
        WeightedEdge edge = new WeightedEdge(start, end, weight);
        addEdge(edge);
      }
    }
  }


  /**
   * Create empty graph, for successively construction
   * @param pNumNodes number of nodes it should contain
   */
  public WeightedGraph(int pNumNodes) {
    numNodes = pNumNodes;
    nodes = new WeightedNode[numNodes];
    outgoingEdges = new HashMap<>(numNodes);
    incomingEdges = new HashMap<>(numNodes);
  }


  /**
   * Insert an edge into structure
   * @param edge edge to be inserted
   */
  private void addEdge(WeightedEdge edge) {
    WeightedNode start = edge.getStartNode();
    int startNumber = start.getNodeNumber();
    WeightedNode end = edge.getEndNode();
    int endNumber = end.getNodeNumber();
    nodes[startNumber] = start;
    nodes[endNumber] = end;

    if (incomingEdges.get(endNumber) == null) {
      incomingEdges.put(endNumber, new HashSet<WeightedEdge>());
    }
    incomingEdges.get(endNumber).add(edge);

    if (outgoingEdges.get(startNumber) == null) {
      outgoingEdges.put(startNumber, new HashSet<WeightedEdge>());
    }
    outgoingEdges.get(startNumber).add(edge);
  }

  /**
   * Get a node via its number, if it is contained in the graph
   * @param node the node number
   * @return The weighted node, or null, if not contained in graph
   */
  @Nullable
  public WeightedNode getNode(int node) {
    return nodes[node];
  }

  public Set<WeightedNode> getPredecessors(int node) {
    Set<WeightedNode> predecessors = new HashSet<>();
    if (incomingEdges.get(node) != null) {
      for (WeightedEdge e : incomingEdges.get(node)) {
        predecessors.add(e.getStartNode());
      }
    }
    return predecessors;
  }

  public Set<WeightedNode> getPredecessors(WeightedNode node) {
    return getPredecessors(node.getNodeNumber());
  }

  public Set<WeightedNode> getSuccessors(int node) {
    Set<WeightedNode> successors = new HashSet<>();
    if (outgoingEdges.get(node) != null) {
      for (WeightedEdge e : outgoingEdges.get(node)) {
        successors.add(e.getEndNode());
      }
    }
    return successors;
  }

  public Set<WeightedNode> getSuccessors(WeightedNode node) {
    return getSuccessors(node.getNodeNumber());
  }

  public Set<Integer> getIntSuccessors(int node) {
    Set<Integer> successors = new HashSet<>();
    if (outgoingEdges.get(node) != null) {
      for (WeightedEdge e : outgoingEdges.get(node)) {
        successors.add(e.getEndNode().getNodeNumber());
      }
    }
    return successors;
  }

  public Set<Integer> getIntSuccessors(WeightedNode node) {
    return getIntSuccessors(node.getNodeNumber());

  }

  public Set<WeightedNode> getNeighbors(WeightedNode node){
    return getNeighbors(node.getNodeNumber());
  }
  public Set <WeightedNode> getNeighbors(int node){
    Set<WeightedNode> neighbors=getSuccessors(node);
    neighbors.addAll(getPredecessors(node));
    return neighbors;
  }



  static public int computeWeight(Set<WeightedNode> nodes) {
    int weight = 0;
    for (WeightedNode node : nodes) {
      weight += node.getWeight();
    }
    return weight;
  }

  static public int computeWeight(Set<Integer> nodes, WeightedGraph wGraph) {
    int weight = 0;
    for (Integer node : nodes) {
      weight += wGraph.getNode(node).getWeight();
    }
    return weight;
  }


  public int getNumNodes() {
    return numNodes;
  }

  public Set<WeightedEdge> getOutgoingEdges(WeightedNode node) {
    return getOutgoingEdges(node.getNodeNumber());
  }

  public Set<WeightedEdge> getOutgoingEdges(int node) {
    Set<WeightedEdge> edges = outgoingEdges.get(node);
    if (edges != null) {
      return edges;
    } else {
      return new HashSet<>(0);
    }
  }

  public Set<WeightedEdge> getIncomingEdges(WeightedNode node) {
    return getIncomingEdges(node.getNodeNumber());
  }

  public Set<WeightedEdge> getIncomingEdges(int node) {
    Set<WeightedEdge> edges = incomingEdges.get(node);
    if (edges != null) {
      return edges;
    } else {
      return new HashSet<>(0);
    }
  }



}
