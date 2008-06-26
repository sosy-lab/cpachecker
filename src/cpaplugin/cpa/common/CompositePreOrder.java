package cpaplugin.cpa.common;

import java.util.List;

import cpaplugin.common.CPAException;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PreOrder;

public class CompositePreOrder implements PreOrder
{
    private List<PreOrder> preOrders;
    
    public CompositePreOrder (List<PreOrder> preOrders)
    {
        this.preOrders = preOrders;
    }
    
    public boolean satisfiesPreOrder (AbstractElement element1, AbstractElement element2) throws CPAException
    {
        CompositeElement comp1 = (CompositeElement) element1;
        CompositeElement comp2 = (CompositeElement) element2;
        
        List<AbstractElement> comp1Elements = comp1.getElements ();
        List<AbstractElement> comp2Elements = comp2.getElements ();
        
        if (comp1Elements.size () != comp2Elements.size ())
            throw new CPAException ("Must check pre-order satisfaction of composite elements of the same size");
        if (comp1Elements.size () != preOrders.size ())
            throw new CPAException ("Wrong number of pre-orders");
                
        for (int idx = 0; idx < comp1Elements.size (); idx++)
        {
            PreOrder preOrder = preOrders.get (idx);
            if (!preOrder.satisfiesPreOrder (comp1Elements.get (idx), comp2Elements.get (idx)))
                return false;
        }
        
        return true;
    }
}
