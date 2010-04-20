package fllesh.fql.fllesh.cpa;

public class QueryTopElement implements QueryElement {

  private static QueryTopElement mInstance = new QueryTopElement();
  
  private QueryTopElement() {
    
  }
  
  public static QueryTopElement getInstance() {
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
    return "Query Top Element";
  }
  
  @Override
  public boolean isError() {
    // TODO Auto-generated method stub
    return false;
  }

}
