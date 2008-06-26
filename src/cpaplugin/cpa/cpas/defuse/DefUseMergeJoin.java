package cpaplugin.cpa.cpas.defuse;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

public class DefUseMergeJoin implements MergeOperator
{
    private DefUseDomain defUseDomain;
    
    public DefUseMergeJoin (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2)
    {
        DefUseElement defUseElement1 = (DefUseElement) element1;
        DefUseElement defUseElement2 = (DefUseElement) element2;
        
        List<DefUseDefinition> mergedDefinitions = new ArrayList<DefUseDefinition> ();
        for (int defIdx = 0; defIdx < defUseElement1.getNumDefinitions (); defIdx++)
            mergedDefinitions.add (defUseElement1.getDefinition (defIdx));
        
        for (int defIdx = 0; defIdx < defUseElement2.getNumDefinitions (); defIdx++)
        {
            DefUseDefinition def = defUseElement2.getDefinition (defIdx);
            if (!mergedDefinitions.contains (def))
                mergedDefinitions.add (def);
        }
        
        return new DefUseElement (mergedDefinitions);
    }
}
