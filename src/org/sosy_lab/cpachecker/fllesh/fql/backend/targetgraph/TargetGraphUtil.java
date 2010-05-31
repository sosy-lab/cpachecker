package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph.TargetGraph.Builder;

public class TargetGraphUtil {

  public static TargetGraph cfa(CFANode pInitialNode) {
    if (pInitialNode == null) {
      throw new IllegalArgumentException();
    }
    
    Builder lBuilder = new Builder();
    
    HashMap<CFANode, Node> lNodeMapping = new HashMap<CFANode, Node>();

    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();

    lWorklist.add(pInitialNode);

    Node lInitialNode = new Node(pInitialNode);
    lBuilder.addInitialNode(lInitialNode);
    lBuilder.addNode(lInitialNode);
    
    lNodeMapping.put(pInitialNode, lInitialNode);

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

        lBuilder.addFinalNode(lNode);
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
            lBuilder.addNode(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          lBuilder.addEdge(lNode, lSuccessorNode, lEdge);
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
            lBuilder.addNode(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          lBuilder.addEdge(lNode, lSuccessorNode, lCallToReturnEdge);
        }
      }
    }
    
    return lBuilder.build();
  }
  
  public static TargetGraph union(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    if (pTargetGraph1 == null || pTargetGraph2 == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder(pTargetGraph1);
    
    lBuilder.addInitialNodes(pTargetGraph2.initialNodes());
    lBuilder.addFinalNodes(pTargetGraph2.finalNodes());
    lBuilder.addNodes(pTargetGraph2.getNodes());
    lBuilder.addEdges(pTargetGraph2.getEdges());
    
    return lBuilder.build();
  }
  
  public static TargetGraph intersect(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    if (pTargetGraph1 == null || pTargetGraph2 == null) {
      throw new IllegalArgumentException();
    }
    
    Builder lBuilder = new Builder();
    
    for (Node lNode : pTargetGraph1.getNodes()) {
      if (pTargetGraph2.contains(lNode)) {
        lBuilder.addNode(lNode);
      }
    }
    
    for (Edge lEdge : pTargetGraph1.getEdges()) {
      if (pTargetGraph2.contains(lEdge)) {
        lBuilder.addEdge(lEdge);
      }
    }
    
    for (Node lInitialNode : pTargetGraph1.initialNodes()) {
      if (pTargetGraph2.isInitialNode(lInitialNode)) {
        lBuilder.addInitialNode(lInitialNode);
      }
    }
    
    for (Node lFinalNode : pTargetGraph1.finalNodes()) {
      if (pTargetGraph2.isFinalNode(lFinalNode)) {
        lBuilder.addFinalNode(lFinalNode);
      }
    }
    
    return lBuilder.build();
  }
  
  public static TargetGraph minus(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    if (pTargetGraph1 == null || pTargetGraph2 == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder();
    
    for (Edge lEdge : pTargetGraph1.getEdges()) {
      if (!pTargetGraph2.contains(lEdge)) {
        lBuilder.addEdge(lEdge);
      }
    }
    
    for (Node lNode : pTargetGraph1.getNodes()) {
      if (!pTargetGraph2.contains(lNode) || pTargetGraph1.getNumberOfOutgoingEdges(lNode) != 0 || pTargetGraph1.getNumberOfIncomingEdges(lNode) != 0) {
        lBuilder.addNode(lNode);
          
        if (pTargetGraph1.isInitialNode(lNode)) {
          lBuilder.addInitialNode(lNode);
        }
        
        if (pTargetGraph1.isFinalNode(lNode)) {
          lBuilder.addFinalNode(lNode);
        }
      }
    }
    
    return lBuilder.build();
  }
  
}
