package org.sosy_lab.cpachecker.cpa.alwaystop;

public class AlwaysTopBottomElement implements AlwaysTopElement {
  private static AlwaysTopBottomElement mInstance = new AlwaysTopBottomElement();
  
  private AlwaysTopBottomElement() {
    
  }
  
  public static AlwaysTopBottomElement getInstance() {
    return mInstance;
  }
  
  @Override
  public String toString() {
    return "FALSE";
  }
  
  @Override
  public boolean isError() {
    // TODO Auto-generated method stub
    return false;
  }

}
