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
public class FunctionCallLabel implements Label<CFAEdge> {
  private String mFunctionName;
  
  public FunctionCallLabel(String pFunctionName) {
    mFunctionName = pFunctionName;
  }

  @Override
  public boolean matches(CFAEdge pEdge) {
    if (CFAEdgeType.FunctionCallEdge == pEdge.getEdgeType()) {
      return pEdge.getSuccessor().getFunctionName().equals(mFunctionName);
    }    

    return false;
  }
  
  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }
    
    if (!(pObject instanceof FunctionCallLabel)) {
      return false;
    }
    
    FunctionCallLabel lLabel = (FunctionCallLabel)pObject;
    
    return mFunctionName.equals(lLabel.mFunctionName);
  }
  
  @Override
  public int hashCode() {
    return mFunctionName.hashCode();
  }
  
  @Override
  public String toString() {
    return "@CALL(" + mFunctionName + ")";
  }
}
