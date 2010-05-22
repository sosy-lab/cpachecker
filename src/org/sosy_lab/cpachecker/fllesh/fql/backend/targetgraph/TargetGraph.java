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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.testgoals.EdgeSequence;
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
  
  public Set<Edge> getOutgoingEdges(Node pNode) {
    return mGraph.outgoingEdgesOf(pNode);
  }
  
  public Iterator<Node> getInitialNodes() {
    return mInitialNodes.iterator();
  }

  public Iterator<Node> getFinalNodes() {
    return mFinalNodes.iterator();
  }

  public static TargetGraph createTargetGraph(EdgeSequence pEdgeSequence) {
    assert(pEdgeSequence != null);
    assert(pEdgeSequence.size() > 1);

    Map<Node, Node> lNodeMap = new HashMap<Node, Node>();

    DirectedGraph<Node, Edge> lGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);

    for (Edge lEdge : pEdgeSequence) {
      Node lOldSourceNode = lEdge.getSource();
      Node lOldTargetNode = lEdge.getTarget();

      Node lSourceNode;
      Node lTargetNode;

      if (!lNodeMap.containsKey(lOldSourceNode)) {
        lSourceNode = new Node(lOldSourceNode);
        lNodeMap.put(lOldSourceNode, lSourceNode);
      }
      else {
        lSourceNode = lNodeMap.get(lOldSourceNode);
      }

      if (!lNodeMap.containsKey(lOldTargetNode)) {
        lTargetNode = new Node(lOldTargetNode);
        lNodeMap.put(lOldTargetNode, lTargetNode);
      }
      else {
        lTargetNode = lNodeMap.get(lOldTargetNode);
      }

      new Edge(lSourceNode, lTargetNode, lEdge.getCFAEdge(), lGraph);
    }

    return new TargetGraph(Collections.singleton(lNodeMap.get(pEdgeSequence.getStartNode())), Collections.singleton(lNodeMap.get(pEdgeSequence.getEndNode())), lGraph);
  }

  public static TargetGraph createTargetGraphFromCFA(CFANode pInitialNode) {
    assert(pInitialNode != null);

    TargetGraph lTargetGraph = new TargetGraph(new HashSet<Node>(), new HashSet<Node>(), new DefaultDirectedGraph<Node, Edge>(Edge.class));

    // create a target graph isomorphic to the given CFA (starting in pInitialNode)
    createTargetGraphFromCFA(pInitialNode, lTargetGraph.mInitialNodes, lTargetGraph.mFinalNodes, lTargetGraph.mGraph);

    return lTargetGraph;
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

  public static TargetGraph applyUnionFilter(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    assert(pTargetGraph1 != null);
    assert(pTargetGraph2 != null);

    TargetGraph lCopy = new TargetGraph(pTargetGraph1);

    lCopy.mInitialNodes.addAll(pTargetGraph2.mInitialNodes);
    lCopy.mFinalNodes.addAll(pTargetGraph2.mFinalNodes);

    for (Node lNode : pTargetGraph2.mGraph.vertexSet()) {
      lCopy.mGraph.addVertex(lNode);
    }

    for (Edge lEdge : pTargetGraph2.mGraph.edgeSet()) {
      Node lSourceNode = pTargetGraph2.mGraph.getEdgeSource(lEdge);
      Node lTargetNode = pTargetGraph2.mGraph.getEdgeTarget(lEdge);

      lCopy.mGraph.addEdge(lSourceNode, lTargetNode, lEdge);
    }

    return lCopy;
  }

  public static TargetGraph applyIntersectionFilter(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    assert(pTargetGraph1 != null);
    assert(pTargetGraph2 != null);

    TargetGraph lCopy = new TargetGraph(pTargetGraph1);

    lCopy.mInitialNodes.retainAll(pTargetGraph2.mInitialNodes);
    lCopy.mFinalNodes.retainAll(pTargetGraph2.mFinalNodes);

    HashSet<Node> lNodesToBeRemoved = new HashSet<Node>();

    for (Node lNode : lCopy.mGraph.vertexSet()) {
      if (!pTargetGraph2.mGraph.containsVertex(lNode)) {
        lNodesToBeRemoved.add(lNode);
      }
    }

    lCopy.mGraph.removeAllVertices(lNodesToBeRemoved);

    HashSet<Edge> lEdgesToBeRemoved = new HashSet<Edge>();

    for (Edge lEdge : lCopy.mGraph.edgeSet()) {
      if (!pTargetGraph2.mGraph.containsEdge(lEdge)) {
        lEdgesToBeRemoved.add(lEdge);
      }
    }

    lCopy.mGraph.removeAllEdges(lEdgesToBeRemoved);

    return lCopy;
  }

  public static TargetGraph applyMinusFilter(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    assert(pTargetGraph1 != null);
    assert(pTargetGraph2 != null);

    TargetGraph lCopy = new TargetGraph(pTargetGraph1);

    lCopy.mGraph.removeAllEdges(pTargetGraph2.mGraph.edgeSet());

    for (Node lNode : pTargetGraph2.mGraph.vertexSet()) {
      if (lCopy.mGraph.inDegreeOf(lNode) == 0 && lCopy.mGraph.outDegreeOf(lNode) == 0) {
        lCopy.mGraph.removeVertex(lNode);

        lCopy.mInitialNodes.remove(lNode);
        lCopy.mFinalNodes.remove(lNode);
      }
    }

    return lCopy;
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

  public static TargetGraph applyPredication(TargetGraph pTargetGraph, Predicates pPredicates) {
    assert(pTargetGraph != null);
    assert(pPredicates != null);

    TargetGraph lResultGraph = pTargetGraph;

    for (Predicate lPredicate : pPredicates) {
      lResultGraph = applyPredication(lResultGraph, lPredicate);
    }

    return lResultGraph;
  }

  private static void createTargetGraphFromCFA(CFANode pInitialNode, Set<Node> pInitialNodes, Set<Node> pFinalNodes, DirectedGraph<Node, Edge> pGraph) {
    assert(pInitialNode != null);
    assert(pInitialNodes.isEmpty());
    assert(pFinalNodes.isEmpty());
    assert(pGraph != null);

    HashMap<CFANode, Node> lNodeMapping = new HashMap<CFANode, Node>();

    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();

    lWorklist.add(pInitialNode);

    Node lInitialNode = new Node(pInitialNode);

    pInitialNodes.add(lInitialNode);

    lNodeMapping.put(pInitialNode, lInitialNode);
    pGraph.addVertex(lInitialNode);

    while (!lWorklist.isEmpty()) {
      CFANode lCFANode = lWorklist.iterator().next();
      lWorklist.remove(lCFANode);

      lVisitedNodes.add(lCFANode);

      Node lNode = lNodeMapping.get(lCFANode);

      // determine successors
      int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();

      CallToReturnEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();

      if (lNumberOfLeavingEdges == 0 && lCallToReturnEdge == null) {
        assert(lCFANode instanceof CFAExitNode);

        pFinalNodes.add(lNode);
      }
      else {
        for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
          CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);
          CFANode lSuccessor = lEdge.getSuccessor();

          Node lSuccessorNode;

          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);

            lNodeMapping.put(lSuccessor, lSuccessorNode);
            pGraph.addVertex(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          new Edge(lNode, lSuccessorNode, lEdge, pGraph);
        }

        if (lCallToReturnEdge != null) {
          CFANode lSuccessor = lCallToReturnEdge.getSuccessor();

          Node lSuccessorNode;

          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);

            lNodeMapping.put(lSuccessor, lSuccessorNode);
            pGraph.addVertex(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          new Edge(lNode, lSuccessorNode, lCallToReturnEdge, pGraph);
        }
      }
    }
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
}
