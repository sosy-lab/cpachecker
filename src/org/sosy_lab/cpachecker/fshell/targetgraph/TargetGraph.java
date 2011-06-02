/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.targetgraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

public class TargetGraph {

  private Set<Node> mInitialNodes;
  private Set<Node> mFinalNodes;
  private DirectedGraph<Node, Edge> mGraph;

  private TargetGraph() {
    mInitialNodes = Collections.emptySet();
    mFinalNodes = Collections.emptySet();
    mGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);
  }

  private TargetGraph(Set<Node> pInitialNodes, Set<Node> pFinalNodes, DirectedGraph<Node, Edge> pGraph) {
    assert(pInitialNodes != null);
    assert(pFinalNodes != null);
    assert(pGraph != null);

    mInitialNodes = pInitialNodes;
    mFinalNodes = pFinalNodes;
    mGraph = pGraph;
  }

  /** copy constructor */
  public TargetGraph(TargetGraph pTargetGraph) {
    assert(pTargetGraph != null);

    mInitialNodes = new LinkedHashSet<Node>(pTargetGraph.mInitialNodes);
    mFinalNodes = new LinkedHashSet<Node>(pTargetGraph.mFinalNodes);
    mGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);

    for (Node lNode : pTargetGraph.mGraph.vertexSet()) {
      mGraph.addVertex(lNode);
    }

    for (Edge lEdge : pTargetGraph.mGraph.edgeSet()) {
      Node lSourceNode = pTargetGraph.mGraph.getEdgeSource(lEdge);
      Node lTargetNode = pTargetGraph.mGraph.getEdgeTarget(lEdge);

      mGraph.addEdge(lSourceNode, lTargetNode, lEdge);
    }
  }

  public TargetGraph(Edge pEdge) {
    assert(pEdge != null);

    Node lSourceNode = new Node(pEdge.getSource());
    Node lTargetNode = new Node(pEdge.getTarget());

    mInitialNodes = Collections.singleton(lSourceNode);
    mFinalNodes = Collections.singleton(lTargetNode);

    mGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);

    new Edge(lSourceNode, lTargetNode, pEdge.getCFAEdge(), mGraph);
  }

  public Set<Node> getNodes() {
    return mGraph.vertexSet();
  }

  public Set<Edge> getEdges() {
    return mGraph.edgeSet();
  }

  public Set<Path> getBoundedPaths(int pBound) {
    if (pBound <= 0) {
      throw new IllegalArgumentException();
    }

    Set<Path> lPaths = new HashSet<Path>();

    LinkedList<Edge> lPrefix = new LinkedList<Edge>();

    Occurrences lOccurrences = new Occurrences();

    for (Node lNode : mInitialNodes) {
      dfs(lNode, lPrefix, lPaths, lOccurrences, pBound);
    }

    return lPaths;
  }

  private void dfs(Node pCurrentNode, LinkedList<Edge> pPrefix, Set<Path> pPaths, Occurrences pOccurrences, int pBound) {
    if (mFinalNodes.contains(pCurrentNode)) {
      pPaths.add(new Path(pCurrentNode, pPrefix));
    }

    for (Edge lOutgoingEdge : getOutgoingEdges(pCurrentNode)) {
      int lOccurrences = pOccurrences.increment(lOutgoingEdge);

      if (lOccurrences <= pBound) {
        pPrefix.addLast(lOutgoingEdge);

        dfs(lOutgoingEdge.getTarget(), pPrefix, pPaths, pOccurrences, pBound);

        pPrefix.pollLast();
      }
      else {
        // TODO change again
        Path lPath = new Path(pCurrentNode, pPrefix);

        pPaths.add(lPath);
      }

      pOccurrences.decrement(lOutgoingEdge);
    }
  }

  public Set<Edge> getOutgoingEdges(Node pNode) {
    return mGraph.outgoingEdgesOf(pNode);
  }

  public int getNumberOfOutgoingEdges(Node pNode) {
    return mGraph.outDegreeOf(pNode);
  }

  public Set<Edge> getIncomingEdges(Node pNode) {
    return mGraph.incomingEdgesOf(pNode);
  }

  public int getNumberOfIncomingEdges(Node pNode) {
    return mGraph.inDegreeOf(pNode);
  }

  public Iterable<Node> initialNodes() {
    return mInitialNodes;
  }

  public Iterable<Node> finalNodes() {
    return mFinalNodes;
  }

  public Iterator<Node> getInitialNodes() {
    return mInitialNodes.iterator();
  }

  public Iterator<Node> getFinalNodes() {
    return mFinalNodes.iterator();
  }

  public boolean contains(Node pNode) {
    return mGraph.vertexSet().contains(pNode);
  }

  public boolean contains(Edge pEdge) {
    return mGraph.edgeSet().contains(pEdge);
  }

  public boolean isInitialNode(Node pNode) {
    return mInitialNodes.contains(pNode);
  }

  public boolean isFinalNode(Node pNode) {
    return mFinalNodes.contains(pNode);
  }

  @Override
  public String toString() {
    String lInitialNodes = "INITIAL NODES: " + mInitialNodes.toString() + "\n";
    String lFinalNodes = "FINAL NODES: " + mFinalNodes.toString() + "\n";

    StringBuffer lBuffer = new StringBuffer();

    lBuffer.append(lInitialNodes);
    lBuffer.append(lFinalNodes);

    for (Edge lEdge : mGraph.edgeSet()) {
      lBuffer.append(lEdge.toString());
      lBuffer.append("\n");
    }

    return lBuffer.toString();
  }

  public static class Builder {

    private static TargetGraph mEmptyGraph = new TargetGraph();
    private boolean mIsCopy = false;
    private TargetGraph mTargetGraph;

    public Builder() {
      mTargetGraph = mEmptyGraph;
    }

    public Builder(TargetGraph pTargetGraph) {
      mTargetGraph = pTargetGraph;
    }

    public Builder(TargetGraph pTargetGraph, MaskFunctor<Node, Edge> pMaskFunctor) {
      DirectedGraph<Node, Edge> lMaskedGraph = new DirectedMaskSubgraph<Node, Edge>(pTargetGraph.mGraph, pMaskFunctor);
      mTargetGraph = new TargetGraph(new LinkedHashSet<Node>(), new LinkedHashSet<Node>(), lMaskedGraph);
    }

    public Set<Edge> edges() {
      return Collections.unmodifiableSet(mTargetGraph.getEdges());
    }

    public Set<Node> nodes() {
      return Collections.unmodifiableSet(mTargetGraph.getNodes());
    }

    public Set<Node> initialNodes() {
      return Collections.unmodifiableSet(mTargetGraph.mInitialNodes);
    }

    public Set<Node> finalNodes() {
      return Collections.unmodifiableSet(mTargetGraph.mFinalNodes);
    }

    public Edge addEdge(Node pSource, Node pTarget, CFAEdge pCFAEdge) {
      if (!mIsCopy) {
        mIsCopy = true;
        mTargetGraph = new TargetGraph(mTargetGraph);
      }

      return new Edge(pSource, pTarget, pCFAEdge, mTargetGraph.mGraph);
    }

    public Edge addEdge(Edge pEdge) {
      return addEdge(pEdge.getSource(), pEdge.getTarget(), pEdge.getCFAEdge());
    }

    public void addEdges(Iterable<Edge> pEdges) {
      for (Edge lEdge : pEdges) {
        addEdge(lEdge);
      }
    }

    public void addNode(Node pNode) {
      if (!mIsCopy) {
        mIsCopy = true;
        mTargetGraph = new TargetGraph(mTargetGraph);
      }

      mTargetGraph.mGraph.addVertex(pNode);
    }

    public void addNodes(Iterable<Node> pNodes) {
      for (Node lNode : pNodes) {
        addNode(lNode);
      }
    }

    public void addInitialNode(Node pNode) {
      addNode(pNode);
      mTargetGraph.mInitialNodes.add(pNode);
    }

    public void addInitialNodes(Iterable<Node> pInitialNodes) {
      for (Node lInitialNode : pInitialNodes) {
        addInitialNode(lInitialNode);
      }
    }

    public void addFinalNode(Node pNode) {
      addNode(pNode);
      mTargetGraph.mFinalNodes.add(pNode);
    }

    public void addFinalNodes(Iterable<Node> pFinalNodes) {
      for (Node lFinalNode : pFinalNodes) {
        addFinalNode(lFinalNode);
      }
    }

    public TargetGraph build() {
      mIsCopy = false;

      return mTargetGraph;
    }

  }

}
