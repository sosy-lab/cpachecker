package org.sosy_lab.cpachecker.cpa.cfapath;

public class CFAPathBottomElement implements CFAPathElement {

  private static final CFAPathBottomElement sInstance = new CFAPathBottomElement();
  
  public static CFAPathBottomElement getInstance() {
    return sInstance;
  }
  
  private CFAPathBottomElement() {
    
  }

}
