package cpaplugin.cpa.cpas.interprocedural;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

public class InterProceduralMergeSep implements MergeOperator
{
    private InterProceduralDomain ipDomain;
    
    public InterProceduralMergeSep (InterProceduralDomain interProDomain)
    {
        this.ipDomain = interProDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return this.ipDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2)
    {
        return element2;
    }
}