package cpaplugin.cpa.common.interfaces;

public interface MergeOperator
{
    public AbstractDomain getAbstractDomain ();
    public AbstractElement merge (AbstractElement element1, AbstractElement element2);
}
