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
  private PrecisionDomain precisionDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public ExplicitAnalysisCPA (String mergeType, String stopType) throws CPAException {
    ExplicitAnalysisDomain explicitAnalysisDomain = new ExplicitAnalysisDomain ();

    this.precisionDomain = new ExplicitAnalysisPrecisionDomain ();

    TransferRelation explicitAnalysisTransferRelation = new ExplicitAnalysisTransferRelation (explicitAnalysisDomain);

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

    this.precisionAdjustment = new ExplicitAnalysisPrecisionAdjustment ();

    this.abstractDomain = explicitAnalysisDomain;
    this.mergeOperator = explicitAnalysisMergeOp;
    this.stopOperator = explicitAnalysisStopOp;
    this.transferRelation = explicitAnalysisTransferRelation;
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  public PrecisionDomain getPrecisionDomain() {
    return precisionDomain;
  }
  

  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  public AbstractElement getInitialElement (CFAFunctionDefinitionNode node)
  {
    return new ExplicitAnalysisElement();
  }

  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    // TODO Auto-generated method stub
    return null;
  }

}
