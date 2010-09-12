package org.sosy_lab.cpachecker.fllesh.cpa.assume;

public class UnconstrainedAssumeElement implements AssumeElement {

  private static final UnconstrainedAssumeElement sInstance = new UnconstrainedAssumeElement();
  
  public static UnconstrainedAssumeElement getInstance() {
    return sInstance;
  }
  
  private UnconstrainedAssumeElement() {
    
  }

}
