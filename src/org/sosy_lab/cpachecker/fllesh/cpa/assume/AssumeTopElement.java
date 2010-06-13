package org.sosy_lab.cpachecker.fllesh.cpa.assume;

public class AssumeTopElement implements AssumeElement {

  private static AssumeTopElement sInstance = new AssumeTopElement();
  
  public static AssumeTopElement getInstance() {
    return sInstance;
  }
  
  private AssumeTopElement() {
    
  }
  
  @Override
  public boolean isError() {
    return false;
  }

}
