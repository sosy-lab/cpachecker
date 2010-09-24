package org.sosy_lab.cpachecker.fllesh.cpa.location;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class LocationBottomElement implements ILocationElement {

  public static final LocationBottomElement INSTANCE = new LocationBottomElement();
  
  private LocationBottomElement() {
    
  }
  
  @Override
  public String toString() {
    return "LocationBottomElement";
  }

  @Override
  public CFANode getLocationNode() {
    throw new UnsupportedOperationException();
  }

}
