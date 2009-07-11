package cpa.common;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElementWithLocation;

public class ErrorElement implements AbstractElementWithLocation {

  private CFANode loc;
  
  public ErrorElement(CFANode pLoc) {
    this.loc = pLoc;
  }
  
  @Override
  public CFANode getLocationNode() {
    return loc;
  }

  @Override
  public String toString() {
    return "ERROR Element @ " + loc.getNodeNumber();
  }
  
}
