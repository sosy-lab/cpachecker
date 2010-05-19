package org.sosy_lab.cpachecker.fllesh.ecp.translators;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPGuard;

public class GuardedLambdaLabel extends GuardedLabel {

  public GuardedLambdaLabel(ECPGuard pGuard) {
    super(pGuard);
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass().equals(getClass())) {
      GuardedLambdaLabel lLabel = (GuardedLambdaLabel)pOther;
      
      return this.getGuards().equals(lLabel.getGuards());
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return this.getGuards().hashCode() + 23409;
  }

  @Override
  public <T> T accept(GuardedLabelVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
