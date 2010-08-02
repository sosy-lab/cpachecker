package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

public class GuardedEdgeAutomatonTopElement implements GuardedEdgeAutomatonElement {

  private static GuardedEdgeAutomatonTopElement mInstance = new GuardedEdgeAutomatonTopElement();
  
  public static GuardedEdgeAutomatonTopElement getInstance() {
    return mInstance;
  }
  
  private GuardedEdgeAutomatonTopElement() {
    
  }

}
