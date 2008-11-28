package cpaplugin.cpa.common.interfaces;

public interface AbstractDomain
{
    public TopElement getTopElement ();
    public BottomElement getBottomElement ();
    public PartialOrder getPreOrder ();
    public JoinOperator getJoinOperator ();
}
