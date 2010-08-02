package org.sosy_lab.cpachecker.fllesh.cpa.assume;

public class AssumeBottomElement implements AssumeElement {

  private static AssumeBottomElement sInstance = new AssumeBottomElement();
  
  public static AssumeBottomElement getInstance() {
    return sInstance;
  }
  
  private AssumeBottomElement() {
    
  }

}
