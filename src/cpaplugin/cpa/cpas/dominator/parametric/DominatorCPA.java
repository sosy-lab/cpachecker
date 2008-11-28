package cpaplugin.cpa.cpas.dominator.parametric;

import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;

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
	
	@Override
	public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
		AbstractElement dominatedInitialElement_tmp = this.cpa.getInitialElement(node);
		
		AbstractElementWithLocation dominatedInitialElement = (AbstractElementWithLocation)dominatedInitialElement_tmp;
		
		DominatorElement initialElement = new DominatorElement(dominatedInitialElement);
			
		initialElement.update(dominatedInitialElement);
		
		return initialElement;
	}

	@Override
	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}

	@Override
	public MergeOperator getMergeOperator() {
		return mergeOperator;
	}

	@Override
	public StopOperator getStopOperator() {
		return stopOperator;
	}

	@Override
	public TransferRelation getTransferRelation() {
		return transferRelation;
	}

}
