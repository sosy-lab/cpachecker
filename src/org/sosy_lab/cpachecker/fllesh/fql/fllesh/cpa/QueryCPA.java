package org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisCPA;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability.Query;

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
