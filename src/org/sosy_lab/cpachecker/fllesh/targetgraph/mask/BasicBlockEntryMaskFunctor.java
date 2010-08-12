package org.sosy_lab.cpachecker.fllesh.targetgraph.mask;

import java.util.Set;

import org.jgrapht.graph.MaskFunctor;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Edge;
import org.sosy_lab.cpachecker.fllesh.targetgraph.Node;

public class BasicBlockEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private Set<CFAEdge> mBasicBlockEntries;
  
  public BasicBlockEntryMaskFunctor(Set<CFAEdge> pBasicBlockEntries) {
    mBasicBlockEntries = pBasicBlockEntries;
  }
  
  @Override
  public boolean isEdgeMasked(Edge pEdge) {
    CFAEdge lCFAEdge = pEdge.getCFAEdge();
    
    return !mBasicBlockEntries.contains(lCFAEdge);
  }

  @Override
  public boolean isVertexMasked(Node pNode) {
    CFANode lCFANode = pNode.getCFANode();
    
    for (int lIndex = 0; lIndex < lCFANode.getNumEnteringEdges(); lIndex++) {
      CFAEdge lCFAEdge = lCFANode.getEnteringEdge(lIndex);
      
      if (mBasicBlockEntries.contains(lCFAEdge)) {
        return false;
      }
    }
    
    for (int lIndex = 0; lIndex < lCFANode.getNumLeavingEdges(); lIndex++) {
      CFAEdge lCFAEdge = lCFANode.getLeavingEdge(lIndex);
      
      if (mBasicBlockEntries.contains(lCFAEdge)) {
        return false;
      }
    }
    
    return true;
  }
  
}
