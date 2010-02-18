package cpa.mustmay;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.alwaystop.AlwaysTopCPA;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.concrete.ConcreteAnalysisCPA;

public class SimpleMustMayAnalysisCPA implements ConfigurableProgramAnalysis {

  private static class SimpleMustMayAnalysisCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new SimpleMustMayAnalysisCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new SimpleMustMayAnalysisCPAFactory();
  }
  
  private final MustMayAnalysisCPA mMustMayAnalysisCPA;
  
  public SimpleMustMayAnalysisCPA() {
    AlwaysTopCPA lMayCPA = new AlwaysTopCPA();
    ConcreteAnalysisCPA lMustCPA = new ConcreteAnalysisCPA();
    
    mMustMayAnalysisCPA = new MustMayAnalysisCPA(lMustCPA, lMayCPA);
  }
  
  @Override
  public MustMayAnalysisElement getInitialElement(
      CFAFunctionDefinitionNode pNode) {
    return mMustMayAnalysisCPA.getInitialElement(pNode);
  }

  @Override
  public MustMayAnalysisDomain getAbstractDomain() {
    return mMustMayAnalysisCPA.getAbstractDomain();
  }

  @Override
  public MustMayAnalysisPrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return mMustMayAnalysisCPA.getInitialPrecision(pNode);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mMustMayAnalysisCPA.getMergeOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return mMustMayAnalysisCPA.getPrecisionAdjustment();
  }

  @Override
  public StopOperator getStopOperator() {
    return mMustMayAnalysisCPA.getStopOperator();
  }

  @Override
  public MustMayAnalysisTransferRelation getTransferRelation() {
    return mMustMayAnalysisCPA.getTransferRelation();
  }

}
