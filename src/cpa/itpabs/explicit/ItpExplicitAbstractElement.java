package cpa.itpabs.explicit;

import java.util.Collection;
import java.util.Collections;

import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;

import cpa.itpabs.ItpAbstractElement;

/**
 * AbstractElement for the Explicit-state version of the interpolation-based
 * lazy abstraction analysis
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpExplicitAbstractElement extends ItpAbstractElement {
    
    public ItpExplicitAbstractElement(CFANode loc) {
        super(loc);
    }

    public String toString() {
        return "E<" + Integer.toString(
                getLocation().getNodeNumber()) + ">(" +
                Integer.toString(getId()) + ",P=" + 
                (getParent() != null ? getParent().getId() : "NIL") + ")"; 
    }

    public boolean isErrorLocation() {
        return (getLocation() instanceof CFAErrorNode);
    }
    
    public Collection<CFANode> getLeaves() {
        return Collections.singleton(getLocation());
    }
    
}
