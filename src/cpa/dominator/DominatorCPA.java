package cpa.dominator;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.location.LocationCPA;
import exceptions.CPAException;

public class DominatorCPA implements ConfigurableProgramAnalysis {

	private cpa.dominator.parametric.DominatorCPA parametricDominatorCPA;

	public DominatorCPA(String mergeType, String stopType) throws CPAException {
		this.parametricDominatorCPA = new cpa.dominator.parametric.DominatorCPA(new LocationCPA(mergeType, stopType));
	}

	public AbstractDomain getAbstractDomain() {
		return this.parametricDominatorCPA.getAbstractDomain();
	}

  public TransferRelation getTransferRelation() {
    return this.parametricDominatorCPA.getTransferRelation();
  }

	public MergeOperator getMergeOperator() {
		return this.parametricDominatorCPA.getMergeOperator();
	}

	public StopOperator getStopOperator() {
		return this.parametricDominatorCPA.getStopOperator();
	}

  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.parametricDominatorCPA.getPrecisionAdjustment();
  }

  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return this.parametricDominatorCPA.getInitialElement(node);
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return this.parametricDominatorCPA.getInitialPrecision(pNode);
  }
}
