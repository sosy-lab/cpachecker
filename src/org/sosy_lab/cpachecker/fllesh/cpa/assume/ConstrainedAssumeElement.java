package org.sosy_lab.cpachecker.fllesh.cpa.assume;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

public class ConstrainedAssumeElement implements AssumeElement {

  private IASTExpression mExpression;
  
  public ConstrainedAssumeElement(IASTExpression pExpression) {
    mExpression = pExpression;
  }
  
  public IASTExpression getExpression() {
    return mExpression;
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!getClass().equals(pOther.getClass())) {
      return false;
    }
    
    ConstrainedAssumeElement lElement = (ConstrainedAssumeElement)pOther;
    
    return lElement.mExpression.equals(mExpression);
  }
  
  @Override
  public int hashCode() {
    return mExpression.hashCode() + 1029;
  }

}
