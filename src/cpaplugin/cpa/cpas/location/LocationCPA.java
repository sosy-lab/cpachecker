package cpaplugin.cpa.cpas.location;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

public class LocationCPA implements ConfigurableProgramAnalysis{

	private AbstractDomain abstractDomain;
	private MergeOperator mergeOperator;
	private StopOperator stopOperator;
	private TransferRelation transferRelation;

	public LocationCPA (String mergeType, String stopType) throws CPAException{
		LocationDomain locationDomain = new LocationDomain ();
        MergeOperator locationMergeOp = null;
        if(mergeType.equals("sep")){
        	locationMergeOp = new LocationMergeSep (locationDomain);
        }
        if(mergeType.equals("join")){
        	throw new CPAException("Location domain elements cannot be joined");
        }
        StopOperator locationStopOp = new LocationStopSep (locationDomain);
        TransferRelation locationTransferRelation = new LocationTransferRelation (locationDomain);
		
		this.abstractDomain = locationDomain;
		this.mergeOperator = locationMergeOp;
		this.stopOperator = locationStopOp;
		this.transferRelation = locationTransferRelation;
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
