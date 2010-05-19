package org.sosy_lab.cpachecker.fllesh.ecp;

public class ECPRepetition implements ElementaryCoveragePattern {

  private ElementaryCoveragePattern mSubpattern;
  
  public ECPRepetition(ElementaryCoveragePattern pSubpattern) {
    mSubpattern = pSubpattern;
  }
  
  public ElementaryCoveragePattern getSubpattern() {
    return mSubpattern;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther instanceof ECPRepetition) {
      ECPRepetition lOther = (ECPRepetition)pOther;
      
      return mSubpattern.equals(lOther.mSubpattern);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mSubpattern.hashCode() + 7879;
  }
  
}
