package cpaplugin.cpa.common;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;

public class SimpleCPA implements ConfigurableProblemAnalysis
{
    private AbstractDomain abstractDomain;
    private MergeOperator mergeOperator;
    private StopOperator stopOperator;
    private TransferRelation transferRelation;
    
    private SimpleCPA (AbstractDomain abstractDomain,
                        MergeOperator mergeOperator,
                        StopOperator stopOperator,
                        TransferRelation transferRelation)
    {
        this.abstractDomain = abstractDomain;
        this.mergeOperator = mergeOperator;
        this.stopOperator = stopOperator;
        this.transferRelation = transferRelation;
    }
    
    public static SimpleCPA createSimpleCPA (AbstractDomain abstractDomain,
                                        MergeOperator mergeOperator,
                                        StopOperator stopOperator,
                                        TransferRelation transferRelation)
    {
        if (abstractDomain == null || mergeOperator == null ||
                stopOperator == null || transferRelation == null)
            return null;
        
        if (mergeOperator.getAbstractDomain () != abstractDomain ||
                stopOperator.getAbstractDomain () != abstractDomain ||
                transferRelation.getAbstractDomain () != abstractDomain)
            return null;
        
        return new SimpleCPA (abstractDomain, mergeOperator, stopOperator, transferRelation);
    }
    
    public AbstractDomain getAbstractDomain ()
    {
        return abstractDomain;
    }
    
    public MergeOperator getMergeOperator ()
    {
        return mergeOperator;
    }

    public StopOperator getStopOperator ()
    {
        return stopOperator;
    }

    public TransferRelation getTransferRelation ()
    {
        return transferRelation;
    }

}
