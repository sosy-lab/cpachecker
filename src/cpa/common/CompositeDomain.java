package cpa.common;

import java.util.ArrayList;
import java.util.List;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public class CompositeDomain implements AbstractDomain
{
    private final List<AbstractDomain> domains;

    private final CompositeBottomElement bottomElement;
    private final CompositeTopElement topElement;
    private final CompositeJoinOperator joinOperator;
    private final CompositePartialOrder partialOrder;

    public CompositeDomain (List<AbstractDomain> domains)
    {
        this.domains = domains;

        List<AbstractElement> bottoms = new ArrayList<AbstractElement> ();
        List<AbstractElement> tops = new ArrayList<AbstractElement> ();
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

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
    }

    public AbstractElement getTopElement ()
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
