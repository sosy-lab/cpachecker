package org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa;

public class QueryBottomElement implements QueryElement {
  
  private static QueryBottomElement mInstance = new QueryBottomElement();
  
  private QueryBottomElement() {
    
  }
  
  public static QueryBottomElement getInstance() {
    return mInstance;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    return (pOther.getClass() == getClass());
  }
  
  @Override
  public int hashCode() {
    return Integer.MAX_VALUE;
  }
  
  @Override
  public String toString() {
    return "Query Bottom Element";
  }
  
  @Override
  public boolean isError() {
    // TODO Auto-generated method stub
    return false;
  }
  
}
