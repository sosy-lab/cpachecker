package cpaplugin.cpa.common.interfaces;

public interface AbstractDomain
{
    public TopElement getTopElement ();
    public BottomElement getBottomElement ();
    public boolean isBottomElement(AbstractElement element);
    public PartialOrder getPartialOrder ();
    public JoinOperator getJoinOperator ();
}
