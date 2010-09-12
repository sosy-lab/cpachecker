package org.sosy_lab.cpachecker.fllesh.cpa.assume;

public class AssumeTopElement implements AssumeElement {

  private static final AssumeTopElement sInstance = new AssumeTopElement();
  
  public static AssumeTopElement getInstance() {
    return sInstance;
  }
  
  private AssumeTopElement() {
    
  }

}
