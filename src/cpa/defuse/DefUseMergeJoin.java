package cpa.defuse;

import java.util.ArrayList;
import java.util.List;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class DefUseMergeJoin implements MergeOperator
{
    private final DefUseDomain defUseDomain;

    public DefUseMergeJoin (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision prec)
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

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
}
