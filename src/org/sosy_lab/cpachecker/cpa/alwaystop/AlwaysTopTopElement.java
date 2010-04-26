package org.sosy_lab.cpachecker.cpa.alwaystop;

public class AlwaysTopTopElement implements AlwaysTopElement {
  private static AlwaysTopTopElement mInstance = new AlwaysTopTopElement();
  
  private AlwaysTopTopElement() {
    
  }
  
  public static AlwaysTopTopElement getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "TRUE";
  }
  
  @Override
  public boolean isError() {
    // TODO Auto-generated method stub
    return false;
  }

}
