package fllesh.fql.fllesh.cpa;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.mustmay.MustMayAnalysisCPA;
import fllesh.fql.fllesh.reachability.Query;

public class QueryCPA implements ConfigurableProgramAnalysis {

  private Query mQuery;
  private MustMayAnalysisCPA mDataSpaceCPA;
  
  private QueryDomain mDomain;
  private QueryTransferRelation mTransferRelation;
  private StopOperator mStopOperator;
  
  public QueryCPA(Query pQuery, MustMayAnalysisCPA pDataSpaceCPA) {
    assert(pQuery != null);
    
    mQuery = pQuery;
    mDataSpaceCPA = pDataSpaceCPA;
    
    mDomain = new QueryDomain(mDataSpaceCPA.getAbstractDomain().getJoinOperator(), mDataSpaceCPA.getAbstractDomain().getPartialOrder());
    mTransferRelation = new QueryTransferRelation(mQuery, mDomain.getTopElement(), mDomain.getBottomElement(), mDataSpaceCPA.getTransferRelation(), mDataSpaceCPA.getAbstractDomain().getBottomElement().getMustElement());
    mStopOperator = new StopSepOperator(mDomain.getPartialOrder());
  }
  
  @Override
  public QueryDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public QueryTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

}
