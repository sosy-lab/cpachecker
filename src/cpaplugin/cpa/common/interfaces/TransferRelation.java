package cpaplugin.cpa.common.interfaces;

import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.common.CPAException;

public interface TransferRelation
{
    public AbstractDomain getAbstractDomain ();
    public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge);
    public List<AbstractElement> getAllAbstractSuccessors (AbstractElement element) throws CPAException;
}
