package org.sosy_lab.cpachecker.fllesh.cpa.location;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class LocationTopElement implements ILocationElement {

  public static final LocationTopElement INSTANCE = new LocationTopElement();
  
  private LocationTopElement() {
    
  }
  
  @Override
  public String toString() {
    return "LocationTopElement";
  }

  @Override
  public CFANode getLocationNode() {
    throw new UnsupportedOperationException();
  }
  
}
