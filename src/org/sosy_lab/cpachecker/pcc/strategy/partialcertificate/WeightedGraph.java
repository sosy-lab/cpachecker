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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Store and access a weighted graph structure
 */
public class WeightedGraph implements Iterable<WeightedNode> {

  private final int numNodes;
  private int totalNodeWeight;
  private final WeightedNode[] nodes;
  /**
   * store for each node its out- and incoming edges (each edge is stored twice)
   */
  private final Map<Integer, Set<WeightedEdge>> outgoingEdges;
  private final Map<Integer, Set<WeightedEdge>> incomingEdges;


  public WeightedGraph(PartialReachedSetDirectedGraph pGraph) {
    if (pGraph == null) { throw new IllegalArgumentException(
        "Graph may not be null."); }
    totalNodeWeight=0;
    numNodes = pGraph.getNumNodes();
    nodes = new WeightedNode[numNodes];
    outgoingEdges = new HashMap<>(numNodes);
    incomingEdges = new HashMap<>(numNodes);

    int weight = 1;
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
    if (pNumNodes <= 0) { throw new IllegalArgumentException(
        "Graph Size may not be 0."); }
    numNodes = pNumNodes;
    nodes = new WeightedNode[numNodes];
    outgoingEdges = new HashMap<>(numNodes);
    incomingEdges = new HashMap<>(numNodes);
  }


/**
 * Inserts a new node into the graph structure. This method automatically updates the graph's total node-weight.
 * @param node node to be inserted into the structure
 */
  public void insertNode(WeightedNode node) {
    int nodeNum=node.getNodeNumber();
    int nodeWeight=node.getWeight();
    if(nodes[nodeNum]!=null){//The node stored before is deleted, so its weight needs to be subtracted
      totalNodeWeight-=nodes[nodeNum].getWeight();
    }
    nodes[nodeNum]=node;
    totalNodeWeight+=nodeWeight;

}


  /**
   * Insert an edge into structure. If edge between both end-nodes already exists, the edge's weight is updated
   * @param edge edge to be inserted; corresponding nodes' weights are changed according to the edge's nodes
   */
  public void addEdge(WeightedEdge edge) {
    WeightedNode start = edge.getStartNode();
    int startNumber = start.getNodeNumber();
    WeightedNode end = edge.getEndNode();
    int endNumber = end.getNodeNumber();
    insertNode(start);
    insertNode(end);
    boolean edgeExisted = false;

    //Check if for the end node already an incoming edge from start node existed
    //==> if yes: change the edges weight
    if (!incomingEdges.containsKey(endNumber)) {//neither start nor end-node had edge until now
      incomingEdges.put(endNumber, new HashSet<WeightedEdge>());
    }
    if (!outgoingEdges.containsKey(startNumber)) {//neither start nor end-node had edge until now
      outgoingEdges.put(startNumber, new HashSet<WeightedEdge>());
    }

    if (!incomingEdges.get(endNumber).isEmpty() && !outgoingEdges.get(startNumber).isEmpty()) {//check if edge between nodes already existed
      Set<WeightedEdge> inEdges = incomingEdges.get(endNumber); //incoming edges for endNode are updated
      for (WeightedEdge inEdge : inEdges) {
        if (inEdge.getStartNode().getNodeNumber() == startNumber) {//edge with same end- and start-node
          inEdge.addWeight(edge.getWeight()); //This updates the edge in outgoing edges as well
          edgeExisted = true;
          break;
        }
      }
    }

    if (!edgeExisted) {
      incomingEdges.get(endNumber).add(edge);
      outgoingEdges.get(startNumber).add(edge);
    }

  }

  /**
   * Insert a whole set of weighted edges into the graph structure
   * @param edges the edges to be inserted
   */
  public void addEdges(Set<WeightedEdge> edges) {
    for (WeightedEdge edge : edges) {
      addEdge(edge);
    }
  }

  public Iterable<WeightedNode> randomIterator() {
    return new Iterable<WeightedNode>() {
      @Override
      public Iterator<WeightedNode> iterator() {
        return new WeightedGraphRandomIterator(WeightedGraph.this);
      }
    };
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

  public Set<WeightedNode> getNeighbors(WeightedNode node) {
    return getNeighbors(node.getNodeNumber());
  }

  public Set<WeightedNode> getNeighbors(int node) {
    Set<WeightedNode> neighbors = getSuccessors(node);
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

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    for (int node = 0; node < numNodes; node++) {
      if(nodes[node]==null){
        continue;
      }
      int nodeWeight = getNode(node).getWeight();
      s.append(node).append("(W:").append(nodeWeight).append("):");
      for (WeightedEdge edge : this.getOutgoingEdges(node)) {
        s.append("--").append(edge.getWeight()).append("-->");
        s.append(edge.getEndNode().getNodeNumber());
      }
      s.append("|| \t");
    }
    return s.toString();
  }

  /**
   * Compute sum of all contained nodes' weights of
   * @return the total node weight of graph
   */
  public int getTotalNodeWeight() {
    return totalNodeWeight;
  }

  /**
   * Iterates in increasing node-number order over all nodes contained in graph
   */
  @Override
  public Iterator<WeightedNode> iterator() {
    return Iterators.forArray(nodes);
  }

  /**
   * Computes a partition's weight, if graph is split up into numPartitions equal parts
   * @param numPartitions The number of partitions to be created
   * @return maximal weight of the partitions
   */
  public int computePartitionLoad(int numPartitions) {
    return this.getTotalNodeWeight() / numPartitions + 1;
  }

  /**
   * Generates a partition consisting of all nodes
   * @return All nodes in one partition
   */
  public List<Set<Integer>> getGraphAsOnePartition() {
    List<Set<Integer>> partitioning = new ArrayList<>(1);
    Set<Integer> partition = new HashSet<>(numNodes);
    for (WeightedNode node : nodes) {
      partition.add(node.getNodeNumber());
    }
    partitioning.add(partition);
    return partitioning;
  }

  /**
   * Generates a partitioning, in which each node has its own partition.
   * If there are more partitions needed, than nodes in graph exist, empty partitions are added
   * @param numPartitions number of Partitions to be at least created. if numNodes<numPartitions==> empty partitions necessary
   * @return A partitioning consisting of numNodes partitions
   */
  public List<Set<Integer>> getNodesSeperatelyPartitioned(int numPartitions) {
    List<Set<Integer>> partitioning = new ArrayList<>(numNodes);
    int currentPartition=0;
    for (WeightedNode node : nodes) {
      Set<Integer> partition = new HashSet<>(1);
      partition.add(node.getNodeNumber());
      partitioning.add(partition);
      currentPartition++;
    }
    while(currentPartition<numPartitions){
      partitioning.add(new HashSet<Integer>(1));
      currentPartition++;
    }
    return partitioning;
  }

}
