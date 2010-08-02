package org.sosy_lab.cpachecker.fllesh.cpa.assume;

public class UnconstrainedAssumeElement implements AssumeElement {

  private static UnconstrainedAssumeElement sInstance = new UnconstrainedAssumeElement();
  
  public static UnconstrainedAssumeElement getInstance() {
    return sInstance;
  }
  
  private UnconstrainedAssumeElement() {
    
  }

}
