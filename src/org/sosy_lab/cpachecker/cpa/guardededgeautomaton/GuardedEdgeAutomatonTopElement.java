package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

public class GuardedEdgeAutomatonTopElement implements GuardedEdgeAutomatonElement {

  private static final GuardedEdgeAutomatonTopElement mInstance = new GuardedEdgeAutomatonTopElement();
  
  public static GuardedEdgeAutomatonTopElement getInstance() {
    return mInstance;
  }
  
  private GuardedEdgeAutomatonTopElement() {
    
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

}
