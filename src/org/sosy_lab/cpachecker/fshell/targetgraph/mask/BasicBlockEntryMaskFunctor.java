package org.sosy_lab.cpachecker.fshell.targetgraph.mask;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;

public class BasicBlockEntryMaskFunctor implements MaskFunctor<Node, Edge> {

  private final Set<CFAEdge> mBasicBlockEntries;
  private final Set<CFANode> mCFANodes;
  
  public BasicBlockEntryMaskFunctor(Set<CFAEdge> pBasicBlockEntries) {
    mBasicBlockEntries = pBasicBlockEntries;
    
    mCFANodes = new HashSet<CFANode>();
    
    for (CFAEdge lCFAEdge : mBasicBlockEntries) {
      mCFANodes.add(lCFAEdge.getPredecessor());
      mCFANodes.add(lCFAEdge.getSuccessor());
    }
  }
  
  @Override
  public boolean isEdgeMasked(Edge pEdge) {
    CFAEdge lCFAEdge = pEdge.getCFAEdge();
    
    return !mBasicBlockEntries.contains(lCFAEdge);
  }

  @Override
  public boolean isVertexMasked(Node pNode) {
    CFANode lCFANode = pNode.getCFANode();
    
    return !mCFANodes.contains(lCFANode);
  }
  
}
