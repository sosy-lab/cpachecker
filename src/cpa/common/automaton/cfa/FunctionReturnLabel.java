/**
 * 
 */
package cpa.common.automaton.cfa;

import cpa.common.automaton.Label;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;

/**
 * @author holzera
 *
 */
public class FunctionReturnLabel implements Label<CFAEdge> {
  private String mFunctionName;
  
  public FunctionReturnLabel(String pFunctionName) {
    mFunctionName = pFunctionName;
  }

  @Override
  public boolean matches(CFAEdge pEdge) {
    if (CFAEdgeType.ReturnEdge == pEdge.getEdgeType()) {
      return pEdge.getPredecessor().getFunctionName().equals(mFunctionName);
    }    

    return false;
  }
  
  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }
    
    if (!(pObject instanceof FunctionReturnLabel)) {
      return false;
    }
    
    FunctionReturnLabel lLabel = (FunctionReturnLabel)pObject;
    
    return mFunctionName.equals(lLabel.mFunctionName);
  }
  
  @Override
  public int hashCode() {
    return mFunctionName.hashCode();
  }
  
  @Override
  public String toString() {
    return "@RETURN(" + mFunctionName + ")";
  }
}
