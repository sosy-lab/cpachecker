package fql.backend.targetgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAExitNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

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
  
  /* copy constructor */
  private TargetGraph(TargetGraph pTargetGraph) {
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
    
    lCopy.mInitialNodes.removeAll(pTargetGraph2.mInitialNodes);
    lCopy.mFinalNodes.removeAll(pTargetGraph2.mFinalNodes);
    
    lCopy.mGraph.removeAllEdges(pTargetGraph2.mGraph.edgeSet());
    lCopy.mGraph.removeAllVertices(pTargetGraph2.mGraph.vertexSet());
    
    return lCopy;
  }
  
  public Iterator<Node> getInitialNodes() {
    return mInitialNodes.iterator();
  }
  
  public Iterator<Node> getFinalNode() {
    return mFinalNodes.iterator();
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
    
    String lEdges = "";
    
    for (Edge lEdge : mGraph.edgeSet()) {
      lEdges += lEdge.toString() + "\n";
    }
    
    return lInitialNodes + lFinalNodes + lEdges;
  }
}
