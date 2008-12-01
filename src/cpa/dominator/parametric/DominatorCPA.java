package cpa.dominator.parametric;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.dominator.parametric.DominatorDomain;
import cpa.dominator.parametric.DominatorElement;
import cpa.dominator.parametric.DominatorMerge;
import cpa.dominator.parametric.DominatorStop;
import cpa.dominator.parametric.DominatorTransferRelation;
import exceptions.CPAException;

public class DominatorCPA implements ConfigurableProgramAnalysis {

	private DominatorDomain abstractDomain;
	private DominatorMerge mergeOperator;
	private DominatorStop stopOperator;
	private DominatorTransferRelation transferRelation;

	private ConfigurableProgramAnalysis cpa;

	public DominatorCPA(ConfigurableProgramAnalysis cpa) throws CPAException {
		this.cpa = cpa;

		this.abstractDomain = new DominatorDomain(this.cpa);
		this.mergeOperator = new DominatorMerge(this.abstractDomain);
		this.stopOperator = new DominatorStop(this.abstractDomain);
		this.transferRelation = new DominatorTransferRelation(this.abstractDomain, this.cpa);
	}

	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		AbstractElement dominatedInitialElement_tmp = this.cpa.getInitialElement(node);

		AbstractElementWithLocation dominatedInitialElement = (AbstractElementWithLocation)dominatedInitialElement_tmp;

		DominatorElement initialElement = new DominatorElement(dominatedInitialElement);

		initialElement.update(dominatedInitialElement);

		return initialElement;
	}

	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}

	public MergeOperator getMergeOperator() {
		return mergeOperator;
	}

	public StopOperator getStopOperator() {
		return stopOperator;
	}

	public TransferRelation getTransferRelation() {
		return transferRelation;
	}

}
