package fql.backend.targetgraph;

import cfa.objectmodel.CFANode;

public class Node {
  private CFANode mCFANode;
  
  // TODO: add reference to list of predicates (or predicate map)
  // TODO: add evaluation of predicates
  
  
  public Node(CFANode pCFANode) {
    assert(pCFANode != null);
    
    mCFANode = pCFANode;
  }
  
  public Node(Node pNode) {
    assert(pNode != null);

    mCFANode = pNode.mCFANode;
  }
  
  public CFANode getCFANode() {
    return mCFANode;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Node lNode = (Node)pOther;
      
      return (lNode.mCFANode == mCFANode);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 293421 + mCFANode.hashCode();
  }
  
  @Override
  public String toString() {
    return "(cfa node: " + mCFANode.toString() + ")";
  }
}
