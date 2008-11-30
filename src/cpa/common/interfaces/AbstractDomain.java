package cpa.common.interfaces;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;

public interface AbstractDomain
{
    public TopElement getTopElement ();
    public BottomElement getBottomElement ();
    public boolean isBottomElement(AbstractElement element);
    public PartialOrder getPartialOrder ();
    public JoinOperator getJoinOperator ();
}
