package cpa.explicit;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class ExplicitAnalysisCPA implements ConfigurableProgramAnalysis {

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;

  public ExplicitAnalysisCPA (String mergeType, String stopType) throws CPAException {
    ExplicitAnalysisDomain explicitAnalysisDomain = new ExplicitAnalysisDomain ();
    MergeOperator explicitAnalysisMergeOp = null;
    if(mergeType.equals("sep")){
      explicitAnalysisMergeOp = new ExplicitAnalysisMergeSep (explicitAnalysisDomain);
    }
    if(mergeType.equals("join")){
      explicitAnalysisMergeOp = new ExplicitAnalysisMergeJoin (explicitAnalysisDomain);
    }

    StopOperator explicitAnalysisStopOp = null;

    if(stopType.equals("sep")){
      explicitAnalysisStopOp = new ExplicitAnalysisStopSep (explicitAnalysisDomain);
    }
    if(stopType.equals("join")){
      explicitAnalysisStopOp = new ExplicitAnalysisStopJoin (explicitAnalysisDomain);
    }

    TransferRelation explicitAnalysisTransferRelation = new ExplicitAnalysisTransferRelation (explicitAnalysisDomain);

    this.abstractDomain = explicitAnalysisDomain;
    this.mergeOperator = explicitAnalysisMergeOp;
    this.stopOperator = explicitAnalysisStopOp;
    this.transferRelation = explicitAnalysisTransferRelation;
    this.precisionAdjustment = new ExplicitAnalysisPrecisionAdjustment();
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

    public AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
    {
        return new ExplicitAnalysisElement();
    }

    @Override
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return new ExplicitAnalysisPrecision();
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return precisionAdjustment;
    }

}
