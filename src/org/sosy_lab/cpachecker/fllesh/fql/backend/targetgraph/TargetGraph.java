/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;

public class TargetGraph {

  private Set<Node> mInitialNodes;
  private Set<Node> mFinalNodes;
  private DirectedGraph<Node, Edge> mGraph;

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

    mInitialNodes = new HashSet<Node>(pTargetGraph.mInitialNodes);
    mFinalNodes = new HashSet<Node>(pTargetGraph.mFinalNodes);
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
  
  private static class Occurrences {
    
    Map<Edge, Integer> mOccurrences = new HashMap<Edge, Integer>();
    
    public Occurrences() {
      
    }
    
    public void decrement(Edge pEdge) {
      if (mOccurrences.containsKey(pEdge)) {
        int lCurrentValue = mOccurrences.get(pEdge);
        
        lCurrentValue--;
        
        if (lCurrentValue < 0) {
          lCurrentValue = 0;
        }
        
        mOccurrences.put(pEdge, lCurrentValue);
      }
      else {
        throw new RuntimeException();
      }
    }
    
    public int increment(Edge pEdge) {
      int lCurrentValue = 0;
      
      if (mOccurrences.containsKey(pEdge)) {
        lCurrentValue = mOccurrences.get(pEdge);
      }

      lCurrentValue++;
     
      mOccurrences.put(pEdge, lCurrentValue);
      
      return lCurrentValue;
    }
    
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
  
  public static class Path {
    
    private Node mStartNode;
    private Node mEndNode;
    private List<Edge> mEdges;
    
    public Path(Node pNode, List<Edge> pEdges) {
      if (pEdges.size() == 0) {
        mStartNode = pNode;
        mEndNode = pNode;
        
        mEdges = Collections.emptyList();
      }
      else {
        mStartNode = pEdges.get(0).getSource();        
        mEndNode = pEdges.get(pEdges.size() - 1).getTarget();
        
        if (!pNode.equals(mEndNode)) {
          throw new IllegalArgumentException();
        }
        
        mEdges = new ArrayList<Edge>(pEdges);
      }
    }
    
    public int length() {
      return mEdges.size();
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      if (!getClass().equals(pOther.getClass())) {
        return false;
      }
      
      Path lOther = (Path)pOther;
      
      return lOther.mStartNode.equals(mStartNode) && lOther.mEndNode.equals(mEndNode) && lOther.mEdges.equals(mEdges);
    }
    
    @Override
    public String toString() {
      StringBuffer lBuffer = new StringBuffer();
      
      lBuffer.append("[");
      
      if (this.length() > 0) {
        boolean lIsFirst = true;
        
        for (Edge lEdge : mEdges) {
          if (lIsFirst) {
            lIsFirst = false;
          }
          else {
            lBuffer.append(", ");
          }
          
          lBuffer.append(lEdge.toString());
        }        
      }
      else {
        lBuffer.append(mStartNode.toString());
      }
      
      lBuffer.append("]");
      
      return lBuffer.toString();
    }
    
  }
  
  public static class Builder {
    
    private TargetGraph mTargetGraph;
    
    public Builder() {
      Set<Node> lInitialNodes = new HashSet<Node>();
      Set<Node> lFinalNodes = new HashSet<Node>();
      DirectedGraph<Node, Edge> lGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);
      
      mTargetGraph = new TargetGraph(lInitialNodes, lFinalNodes, lGraph);
    }
    
    public Builder(TargetGraph pTargetGraph) {
      mTargetGraph = new TargetGraph(pTargetGraph);
    }
    
    public Edge addEdge(Node pSource, Node pTarget, CFAEdge pCFAEdge) {
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
      return new TargetGraph(mTargetGraph);
    }
    
  }
  
  /*
   * Returns a target graph that retains all nodes and edges in pTargetGraph that
   * belong the the function given by pFunctionName. The set of initial nodes is
   * changed to the set of nodes in the resulting target graph that contain a
   * CFAFunctionDefinitionNode. The set of final nodes is changed to the set of
   * nodes in the resulting target graph that contain a CFAExitNode.
   */
  public static TargetGraph applyFunctionNameFilter(TargetGraph pTargetGraph, String pFunctionName) {
    assert(pTargetGraph != null);
    assert(pFunctionName != null);

    MaskFunctor<Node, Edge> lMaskFunctor = new FunctionNameMaskFunctor(pFunctionName);

    DirectedGraph<Node, Edge> lMaskedGraph = new DirectedMaskSubgraph<Node, Edge>(pTargetGraph.mGraph, lMaskFunctor);

    HashSet<Node> lInitialNodes = new HashSet<Node>();
    HashSet<Node> lFinalNodes = new HashSet<Node>();

    for (Node lNode : lMaskedGraph.vertexSet()) {
      CFANode lCFANode = lNode.getCFANode();

      if (lCFANode instanceof CFAFunctionDefinitionNode) {
        lInitialNodes.add(lNode);
      }

      if (lCFANode instanceof CFAExitNode) {
        lFinalNodes.add(lNode);
      }
    }

    return new TargetGraph(lInitialNodes, lFinalNodes, lMaskedGraph);
  }

  public static TargetGraph applyStandardEdgeBasedFilter(TargetGraph pTargetGraph, MaskFunctor<Node, Edge> pMaskFunctor) {
    assert(pTargetGraph != null);
    assert(pMaskFunctor != null);

    DirectedGraph<Node, Edge> lMaskedGraph = new DirectedMaskSubgraph<Node, Edge>(pTargetGraph.mGraph, pMaskFunctor);

    HashSet<Node> lInitialNodes = new HashSet<Node>();
    HashSet<Node> lFinalNodes = new HashSet<Node>();

    for (Edge lEdge : lMaskedGraph.edgeSet()) {
      lInitialNodes.add(lEdge.getSource());
      lFinalNodes.add(lEdge.getTarget());
    }

    return new TargetGraph(lInitialNodes, lFinalNodes, lMaskedGraph);
  }

  public static TargetGraph applyPredication(TargetGraph pTargetGraph, Predicates pPredicates) {
    assert(pTargetGraph != null);
    assert(pPredicates != null);

    TargetGraph lResultGraph = pTargetGraph;

    for (Predicate lPredicate : pPredicates) {
      lResultGraph = applyPredication(lResultGraph, lPredicate);
    }

    return lResultGraph;
  }
  
  public static TargetGraph applyPredication(TargetGraph pTargetGraph, Predicate pPredicate) {
    assert(pTargetGraph != null);
    assert(pPredicate != null);

    HashSet<Node> lInitialNodes = new HashSet<Node>();
    HashSet<Node> lFinalNodes = new HashSet<Node>();
    DirectedGraph<Node, Edge> lGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);

    // 1) duplicate vertices

    HashMap<Node, Pair<Node, Node>> lMap = new HashMap<Node, Pair<Node, Node>>();

    for (Node lNode : pTargetGraph.mGraph.vertexSet()) {
      Node lTrueNode = new Node(lNode);
      lTrueNode.addPredicate(pPredicate, true);
      lGraph.addVertex(lTrueNode);

      Node lFalseNode = new Node(lNode);
      lFalseNode.addPredicate(pPredicate, false);
      lGraph.addVertex(lFalseNode);

      Pair<Node, Node> lPair = new Pair<Node, Node>(lTrueNode, lFalseNode);

      lMap.put(lNode, lPair);
    }

    for (Node lNode : pTargetGraph.mInitialNodes) {
      Pair<Node, Node> lPair = lMap.get(lNode);

      lInitialNodes.add(lPair.getFirst());
      lInitialNodes.add(lPair.getSecond());
    }

    for (Node lNode : pTargetGraph.mFinalNodes) {
      Pair<Node, Node> lPair = lMap.get(lNode);

      lFinalNodes.add(lPair.getFirst());
      lFinalNodes.add(lPair.getSecond());
    }

    // 2) replicate edges

    for (Edge lEdge : pTargetGraph.mGraph.edgeSet()) {
      Node lSourceNode = lEdge.getSource();
      Pair<Node, Node> lSourcePair = lMap.get(lSourceNode);

      Node lTargetNode = lEdge.getTarget();
      Pair<Node, Node> lTargetPair = lMap.get(lTargetNode);

      Node lSourceTrueNode = lSourcePair.getFirst();
      Node lSourceFalseNode = lSourcePair.getSecond();

      Node lTargetTrueNode = lTargetPair.getFirst();
      Node lTargetFalseNode = lTargetPair.getSecond();

      new Edge(lSourceTrueNode, lTargetTrueNode, lEdge.getCFAEdge(), lGraph);
      new Edge(lSourceTrueNode, lTargetFalseNode, lEdge.getCFAEdge(), lGraph);
      new Edge(lSourceFalseNode, lTargetTrueNode, lEdge.getCFAEdge(), lGraph);
      new Edge(lSourceFalseNode, lTargetFalseNode, lEdge.getCFAEdge(), lGraph);
    }

    return new TargetGraph(lInitialNodes, lFinalNodes, lGraph);
  }

}
