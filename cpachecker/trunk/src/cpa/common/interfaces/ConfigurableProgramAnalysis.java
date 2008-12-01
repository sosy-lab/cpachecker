package cpa.common.interfaces;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cfa.objectmodel.CFAFunctionDefinitionNode;

public interface ConfigurableProgramAnalysis
{
	public AbstractDomain getAbstractDomain();
    public TransferRelation getTransferRelation ();
    public MergeOperator getMergeOperator ();
    public StopOperator getStopOperator ();
    public AbstractElement getInitialElement (CFAFunctionDefinitionNode node);
}
