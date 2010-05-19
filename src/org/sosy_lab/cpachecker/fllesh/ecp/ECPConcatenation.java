package org.sosy_lab.cpachecker.fllesh.ecp;

import java.util.Iterator;
import java.util.LinkedList;

public class ECPConcatenation implements ElementaryCoveragePattern, Iterable<ElementaryCoveragePattern> {

  private LinkedList<ElementaryCoveragePattern> mSubpatterns;
  
  public ECPConcatenation(ElementaryCoveragePattern pFirstSubpattern, ElementaryCoveragePattern pSecondSubpattern) {
    mSubpatterns = new LinkedList<ElementaryCoveragePattern>();
    
    if (pFirstSubpattern instanceof ECPConcatenation) {
      ECPConcatenation lFirstSubpattern = (ECPConcatenation)pFirstSubpattern;
      
      mSubpatterns.addAll(lFirstSubpattern.mSubpatterns);
    }
    else {
      mSubpatterns.add(pFirstSubpattern);
    }
    
    if (pSecondSubpattern instanceof ECPConcatenation) {
      ECPConcatenation lSecondSubpattern = (ECPConcatenation)pSecondSubpattern;
      
      mSubpatterns.addAll(lSecondSubpattern.mSubpatterns);
    }
    else {
      mSubpatterns.add(pSecondSubpattern);
    }
  }
  
  @Override
  public Iterator<ElementaryCoveragePattern> iterator() {
    return mSubpatterns.iterator();
  }
  
  public int size() {
    return mSubpatterns.size();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther instanceof ECPConcatenation) {
      ECPConcatenation lOther = (ECPConcatenation)pOther;
      
      return mSubpatterns.equals(lOther.mSubpatterns);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mSubpatterns.hashCode();
  }
  
  @Override
  public String toString() {
    StringBuffer lResult = new StringBuffer();
    
    boolean isFirst = true;
    
    for (ElementaryCoveragePattern lSubpattern : mSubpatterns) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lResult.append(".");
      }
      
      lResult.append(lSubpattern.toString());
    }
    
    return lResult.toString();
  }
  
}
