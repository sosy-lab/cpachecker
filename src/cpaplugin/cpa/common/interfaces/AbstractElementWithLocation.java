package cpaplugin.cpa.common.interfaces;

import cpaplugin.cfa.objectmodel.CFANode;

public interface AbstractElementWithLocation extends AbstractElement{
    public CFANode getLocationNode();
}
