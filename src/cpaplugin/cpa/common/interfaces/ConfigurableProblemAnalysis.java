package cpaplugin.cpa.common.interfaces;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;

public interface ConfigurableProblemAnalysis
{
	public AbstractDomain getAbstractDomain();
    public TransferRelation getTransferRelation ();
    public MergeOperator getMergeOperator ();
    public StopOperator getStopOperator (); 
    public AbstractElement getInitialElement (CFAFunctionDefinitionNode node);
}
