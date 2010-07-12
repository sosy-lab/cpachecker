package org.sosy_lab.cpachecker.fllesh.cpa.cfapath;

public class CFAPathTopElement implements CFAPathElement {

  private static CFAPathTopElement sInstance = new CFAPathTopElement();
  
  public static CFAPathTopElement getInstance() {
    return sInstance;
  }
  
  private CFAPathTopElement() {
    
  }
  
  @Override
  public boolean isError() {
    return false;
  }

}
