package cpaplugin.cpa.common;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

public class CompositeDomain implements AbstractDomain
{
    private List<AbstractDomain> domains;
    
    private CompositeBottomElement bottomElement;
    private CompositeTopElement topElement;
    private CompositeJoinOperator joinOperator;
    private CompositePreOrder preOrder;
       
    public CompositeDomain (List<AbstractDomain> domains)
    {
        this.domains = domains;
        
        List<BottomElement> bottoms = new ArrayList<BottomElement> ();
        List<TopElement> tops = new ArrayList<TopElement> ();
        List<JoinOperator> joinOperators = new ArrayList<JoinOperator> ();
        List<PartialOrder> preOrders = new ArrayList<PartialOrder> ();
        
        for (AbstractDomain domain : domains)
        {
            bottoms.add (domain.getBottomElement ());
            tops.add (domain.getTopElement ());
            joinOperators.add (domain.getJoinOperator ());
            preOrders.add (domain.getPreOrder ());
        }
        
        this.bottomElement = new CompositeBottomElement (bottoms);
        this.topElement = new CompositeTopElement (tops);
        this.joinOperator = new CompositeJoinOperator (joinOperators);
        this.preOrder = new CompositePreOrder (preOrders);
    }
    
    public List<AbstractDomain> getDomains ()
    {
        return domains;
    }
    
    public BottomElement getBottomElement ()
    {
        return bottomElement;
    }

    public TopElement getTopElement ()
    {
        return topElement;
    }
    
    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PartialOrder getPreOrder ()
    {
        return preOrder;
    }  
}
