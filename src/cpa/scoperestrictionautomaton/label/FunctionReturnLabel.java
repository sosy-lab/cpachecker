/**
 * 
 */
package cpa.scoperestrictionautomaton.label;

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
}
