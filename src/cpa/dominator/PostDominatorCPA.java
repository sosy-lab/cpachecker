package cpa.dominator;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.location.InverseLocationCPA;
import exceptions.CPAException;

public class PostDominatorCPA implements ConfigurableProgramAnalysis {

	private cpa.dominator.parametric.DominatorCPA parametricDominatorCPA; 
	
	public PostDominatorCPA(String mergeType, String stopType) throws CPAException {
		this.parametricDominatorCPA = new cpa.dominator.parametric.DominatorCPA(new InverseLocationCPA(mergeType, stopType));
	}
	
	public AbstractDomain getAbstractDomain() {
		return this.parametricDominatorCPA.getAbstractDomain();
	}

	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		return this.parametricDominatorCPA.getInitialElement(node);
	}

	public MergeOperator getMergeOperator() {
		return this.parametricDominatorCPA.getMergeOperator();
	}

	public StopOperator getStopOperator() {
		return this.parametricDominatorCPA.getStopOperator();
	}

	public TransferRelation getTransferRelation() {
		return this.parametricDominatorCPA.getTransferRelation();
	}

}
