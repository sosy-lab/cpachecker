package cpaplugin.cpa.cpas.itpabs;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;

/**
 * An ItpAbstractElementManager is an object that know how to create and
 * manipulate generic ItpAbstractElement. This is used to encapsulate the
 * differences between explicit-state and symbolic (with summaries) versions
 * of the analysis.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface ItpAbstractElementManager {
    public ItpAbstractElement create(CFANode location);
    
    public boolean isFunctionEnd(ItpAbstractElement e, ItpAbstractElement succ);
    public boolean isFunctionStart(ItpAbstractElement e);
    public boolean isRightEdge(ItpAbstractElement e, CFAEdge edge,
                               ItpAbstractElement succ);
    public void pushContextFindRetNode(ItpAbstractElement e, 
                                       ItpAbstractElement succ);
}
