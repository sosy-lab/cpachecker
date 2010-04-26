package org.sosy_lab.cpachecker.fllesh.fql.fllesh;

public class InfeasibilityWitness implements Witness {
  private int mBacktrackIndex;
  
  public InfeasibilityWitness(int pBacktrackIndex) {
    mBacktrackIndex = pBacktrackIndex;
  }
  
  public int getBacktrackIndex() {
    return mBacktrackIndex;
  }
  
}
