package cpa.common;

import java.util.List;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.CompositeElement;
import exceptions.CPAException;

public class CompositePartialOrder implements PartialOrder
{
    private List<PartialOrder> partialOrders;
    
    public CompositePartialOrder (List<PartialOrder> partialOrders)
    {
        this.partialOrders = partialOrders;
    }
    
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2) throws CPAException
    {
        CompositeElement comp1 = (CompositeElement) element1;
        CompositeElement comp2 = (CompositeElement) element2;
        
        List<AbstractElement> comp1Elements = comp1.getElements ();
        List<AbstractElement> comp2Elements = comp2.getElements ();
        
        if (comp1Elements.size () != comp2Elements.size ())
            throw new CPAException ("Must check pre-order satisfaction of composite elements of the same size");
        if (comp1Elements.size () != partialOrders.size ())
            throw new CPAException ("Wrong number of pre-orders");
                
        for (int idx = 0; idx < comp1Elements.size (); idx++)
        {
            PartialOrder partialOrder = partialOrders.get (idx);
            if (!partialOrder.satisfiesPartialOrder (comp1Elements.get (idx), comp2Elements.get (idx)))
                return false;
        }
        
        return true;
    }
}
