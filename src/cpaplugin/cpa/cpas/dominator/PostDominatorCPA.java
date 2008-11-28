package cpaplugin.cpa.cpas.dominator;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.cpa.cpas.location.InverseLocationCPA;

public class PostDominatorCPA implements ConfigurableProgramAnalysis {

	private cpaplugin.cpa.cpas.dominator.parametric.DominatorCPA parametricDominatorCPA; 
	
	public PostDominatorCPA(String mergeType, String stopType) throws CPAException {
		this.parametricDominatorCPA = new cpaplugin.cpa.cpas.dominator.parametric.DominatorCPA(new InverseLocationCPA(mergeType, stopType));
	}
	
	@Override
	public AbstractDomain getAbstractDomain() {
		return this.parametricDominatorCPA.getAbstractDomain();
	}

	@Override
	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		return this.parametricDominatorCPA.getInitialElement(node);
	}

	@Override
	public MergeOperator getMergeOperator() {
		return this.parametricDominatorCPA.getMergeOperator();
	}

	@Override
	public StopOperator getStopOperator() {
		return this.parametricDominatorCPA.getStopOperator();
	}

	@Override
	public TransferRelation getTransferRelation() {
		return this.parametricDominatorCPA.getTransferRelation();
	}

}
