package org.sosy_lab.cpachecker.cpa.assume;

public class AssumeBottomElement implements AssumeElement {

  private static final AssumeBottomElement sInstance = new AssumeBottomElement();
  
  public static AssumeBottomElement getInstance() {
    return sInstance;
  }
  
  private AssumeBottomElement() {
    
  }

}
