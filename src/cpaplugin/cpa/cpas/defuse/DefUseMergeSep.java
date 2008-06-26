package cpaplugin.cpa.cpas.defuse;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

public class DefUseMergeSep implements MergeOperator
{
    private DefUseDomain defUseDomain;
    
    public DefUseMergeSep (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2)
    {
        return element2;
    }
}
