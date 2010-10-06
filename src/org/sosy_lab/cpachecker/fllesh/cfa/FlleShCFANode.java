package org.sosy_lab.cpachecker.fllesh.cfa;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class FlleShCFANode extends CFANode {

  public final static int FlleShLineNumber = Integer.MIN_VALUE;
  
  private final int mOldNodeNumber;
  
  public FlleShCFANode(CFANode pNode) {
    // TODO should we use the line number of pNode ?
    // TODO should we copy use the node number of pNode ?
    super(FlleShLineNumber, pNode.getFunctionName());
    mOldNodeNumber = pNode.getNodeNumber();
  }
  
  @Override
  public boolean equals(Object pOther) {
    return (this == pOther);
  }
  
  @Override
  public int hashCode() {
    return super.hashCode() + 98324;
  }
  
  @Override
  public String toString() {
    return "F" + super.getNodeNumber() + " (" + mOldNodeNumber + ")";
  }
  
  @Override
  public int getLineNumber() {
    // this is an internal temporary node, nothing should depend on its line number
    throw new UnsupportedOperationException();
  }

  @Override
  public int getTopologicalSortId() {
    // this is an internal temporary node, nothing should depend on its topological sort id
    throw new UnsupportedOperationException();
  }

}
