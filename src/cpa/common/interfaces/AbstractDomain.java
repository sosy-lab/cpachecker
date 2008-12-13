package cpa.common.interfaces;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public interface AbstractDomain
{
    public AbstractElement getTopElement ();
    public AbstractElement getBottomElement ();
    public PartialOrder getPartialOrder ();
    public JoinOperator getJoinOperator ();
}
