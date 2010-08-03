package org.sosy_lab.cpachecker.fllesh.targetgraph.mask;

import org.jgrapht.graph.MaskFunctor;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Node;

public class BasicBlockEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private static BasicBlockEntryMaskFunctor sInstance = new BasicBlockEntryMaskFunctor();
  
  public static BasicBlockEntryMaskFunctor getInstance() {
    return sInstance;
  }
  
  private BasicBlockEntryMaskFunctor() {
    
  }
  
  @Override
  public boolean isEdgeMasked(Edge pEdge) {
    CFAEdge lCFAEdge = pEdge.getCFAEdge();
    
    if (isBasicBlockEntry(lCFAEdge)) {
      System.out.println("BBENTRY: " + lCFAEdge.toString());
      
      return false;
    }
    
    return true;
    //return !isBasicBlockEntry(pEdge.getCFAEdge());
  }

  @Override
  public boolean isVertexMasked(Node pNode) {
    CFANode lCFANode = pNode.getCFANode();
    
    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      CFAEdge lCFAEdge = lCFANode.getEnteringEdge(lIndex);
      
      if (isBasicBlockEntry(lCFAEdge)) {
        return false;
      }
    }
    
    for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
      CFAEdge lCFAEdge = lCFANode.getLeavingEdge(lIndex);
      
      if (isBasicBlockEntry(lCFAEdge)) {
        return false;
      }
    }
    
    return true;
  }
  
  private boolean isBasicBlockEntry(CFAEdge pCFAEdge) {
    //System.out.println("CHECKING " +pCFAEdge.toString());
    
    if (isFunctionEntryEdge(pCFAEdge)) {
      //System.out.println(pCFAEdge.toString() + "is fentry");
      
      return true;
    }
    
    if (isSplitEdge(pCFAEdge)) {
      //System.out.println(pCFAEdge.toString() + " is a split edge");
      return false;
    }
    
    if (isJoinEdge(pCFAEdge)) {
      //System.out.println(pCFAEdge.toString() + " is a join edge");
      return false;
    }
    
    if (pCFAEdge.getPredecessor().getNumEnteringEdges() != 1) {
      //System.out.println(pCFAEdge.toString() + " has more then one predecessor edges");
      
      return true;
    }
    
    CFAEdge pPredecessorEdge = pCFAEdge.getPredecessor().getEnteringEdge(0);
    
    boolean lIsSplitingEdge = isSplitEdge(pPredecessorEdge);
    
    /*if (lIsSplitingEdge) {
      System.out.println(pCFAEdge.toString() + " is splitting edge");
    }*/
    
    return lIsSplitingEdge;
  }
  
  private boolean isFunctionEntryEdge(CFAEdge pEdge) {
    return (pEdge.getPredecessor() instanceof FunctionDefinitionNode);
  }
  
  private boolean isSplitEdge(CFAEdge pCFAEdge) {
    return (pCFAEdge.getPredecessor().getNumLeavingEdges() > 1);
  }
  
  private boolean isJoinEdge(CFAEdge pCFAEdge) {
    CFANode lSuccessor = pCFAEdge.getSuccessor();
    
    /*System.out.println(pCFAEdge.toString() + " : " + lSuccessor.getNumEnteringEdges());
    
    if (lSuccessor.getNodeNumber() == 10) {
      for (int lIndex = 0; lIndex < lSuccessor.getNumEnteringEdges(); lIndex++) {
        System.out.println("-> " + lSuccessor.getEnteringEdge(lIndex));
      }
    }*/
    
    return (lSuccessor.getNumEnteringEdges() > 1);
  }

}
