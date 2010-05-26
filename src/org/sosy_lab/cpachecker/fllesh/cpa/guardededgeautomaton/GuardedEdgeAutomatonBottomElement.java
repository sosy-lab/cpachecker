package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

public class GuardedEdgeAutomatonBottomElement implements GuardedEdgeAutomatonElement {

  private static GuardedEdgeAutomatonBottomElement mInstance = new GuardedEdgeAutomatonBottomElement();
  
  public static GuardedEdgeAutomatonBottomElement getInstance() {
    return mInstance;
  }
  
  private GuardedEdgeAutomatonBottomElement() {
    
  }
  
  @Override
  public boolean isError() {
    return false;
  }

}
