package cpaplugin.cpa.cpas.location;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.compositeCPA.MergeType;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProblemAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class LocationCPA implements ConfigurableProblemAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	private LocationCPA (AbstractDomain abstractDomain,
			MergeOperator mergeOperator,
			StopOperator stopOperator,
			TransferRelation transferRelation)
	{
		this.abstractDomain = abstractDomain;
		this.mergeOperator = mergeOperator;
		this.stopOperator = stopOperator;
		this.transferRelation = transferRelation;
	}

	public static LocationCPA createLocationCPA (AbstractDomain abstractDomain,
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

		return new LocationCPA (abstractDomain, mergeOperator, stopOperator, transferRelation);
	}
	
	public static LocationCPA createNewLocationCPA (MergeType mergeType) throws CPAException{
		LocationDomain locationDomain = new LocationDomain ();
        MergeOperator locationMergeOp = null;
        if(mergeType == MergeType.MergeSep){
        	locationMergeOp = new LocationMergeSep (locationDomain);
        }
        if(mergeType == MergeType.MergeJoin){
        	throw new CPAException("Location domain cannot be joined");
        }
        StopOperator locationStopOp = new LocationStopSep (locationDomain);
        TransferRelation locationTransferRelation = new LocationTransferRelation (locationDomain);
		
		return new LocationCPA (locationDomain, locationMergeOp, locationStopOp, locationTransferRelation);
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

	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		return new LocationElement (node);
	}
}
