package cpa.dominator.parametric;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.dominator.parametric.DominatorDomain;
import cpa.dominator.parametric.DominatorElement;
import cpa.dominator.parametric.DominatorMerge;
import cpa.dominator.parametric.DominatorStop;
import cpa.dominator.parametric.DominatorTransferRelation;
import exceptions.CPAException;

public class DominatorCPA implements ConfigurableProgramAnalysis {
  
  private ConfigurableProgramAnalysis cpa;
  
  private DominatorDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

	public DominatorCPA(ConfigurableProgramAnalysis cpa) throws CPAException {
	  this.cpa = cpa;
	  
		this.abstractDomain = new DominatorDomain(this.cpa);
    this.transferRelation = new DominatorTransferRelation(this.abstractDomain, this.cpa);
    this.mergeOperator = new DominatorMerge(this.abstractDomain);
		this.stopOperator = new DominatorStop(this.abstractDomain);
		this.precisionAdjustment = new DominatorPrecisionAdjustment();
	}

	public AbstractDomain getAbstractDomain() {
		return abstractDomain;
	}

  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

	public MergeOperator getMergeOperator() {
		return mergeOperator;
	}

	public StopOperator getStopOperator() {
		return stopOperator;
	}

  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }
	
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    AbstractElement dominatedInitialElement_tmp = this.cpa.getInitialElement(node);

    AbstractElementWithLocation dominatedInitialElement = (AbstractElementWithLocation)dominatedInitialElement_tmp;

    DominatorElement initialElement = new DominatorElement(dominatedInitialElement);

    initialElement.update(dominatedInitialElement);

    return initialElement;
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return new DominatorPrecision();
  }
}
