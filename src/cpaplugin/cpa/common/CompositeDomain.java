package cpaplugin.cpa.common;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
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
    private CompositePartialOrder partialOrder;
       
    public CompositeDomain (List<AbstractDomain> domains)
    {
        this.domains = domains;
        
        List<BottomElement> bottoms = new ArrayList<BottomElement> ();
        List<TopElement> tops = new ArrayList<TopElement> ();
        List<JoinOperator> joinOperators = new ArrayList<JoinOperator> ();
        List<PartialOrder> partialOrders = new ArrayList<PartialOrder> ();
        
        for (AbstractDomain domain : domains)
        {
            bottoms.add (domain.getBottomElement ());
            tops.add (domain.getTopElement ());
            joinOperators.add (domain.getJoinOperator ());
            partialOrders.add (domain.getPartialOrder ());
        }
        
        this.bottomElement = new CompositeBottomElement (bottoms);
        this.topElement = new CompositeTopElement (tops);
        this.joinOperator = new CompositeJoinOperator (joinOperators);
        this.partialOrder = new CompositePartialOrder (partialOrders);
    }
    
    public List<AbstractDomain> getDomains ()
    {
        return domains;
    }
    
    public BottomElement getBottomElement ()
    {
        return bottomElement;
    }
    
    public boolean isBottomElement(AbstractElement element) {

		if(element instanceof BottomElement){
			return true;
		}

		CompositeElement compositeElement = (CompositeElement) element;
		
		List<AbstractElement> compositeElements = compositeElement.getElements ();

		for (int idx = 0; idx < compositeElements.size (); idx++)
		{
			AbstractDomain absDom = domains.get(idx);
			AbstractElement absElem = compositeElements.get(idx);
			if (absDom.isBottomElement(absElem))
				return true;
		}

		return false;
	}

    public TopElement getTopElement ()
    {
        return topElement;
    }
    
    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PartialOrder getPartialOrder ()
    {
        return partialOrder;
    }  
}
