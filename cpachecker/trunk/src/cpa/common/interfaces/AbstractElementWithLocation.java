package cpa.common.interfaces;

import cpa.common.interfaces.AbstractElement;
import cfa.objectmodel.CFANode;

public interface AbstractElementWithLocation extends AbstractElement{
    public CFANode getLocationNode();
}
