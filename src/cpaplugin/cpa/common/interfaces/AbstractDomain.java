package cpaplugin.cpa.common.interfaces;

public interface AbstractDomain
{
    public TopElement getTopElement ();
    public BottomElement getBottomElement ();
    public PreOrder getPreOrder ();
    public JoinOperator getJoinOperator ();
}
