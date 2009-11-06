package cpa.errorlocation;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.EqualityPartialOrder;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisisonAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

public class ErrorLocationCPA implements ConfigurableProgramAnalysis {

  private static enum ErrorLocationElement implements AbstractElement {
    
    NORMAL(false),
    ERROR(true),
    TOP(true),
    BOTTOM(false);
    
    private final boolean isError;
    
    private ErrorLocationElement(boolean isError) {
      this.isError = isError;
    }
    
    @Override
    public String toString() {
      return "<" + super.toString() + ">";
    }
    
    @Override
    public boolean isError() {
      return isError;
    }
  }
  
  private static final JoinOperator joinOperator = new JoinOperator() {
    
    @Override
    public AbstractElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      if (element1 == element2) {
        return element1;
      } else {
        return ErrorLocationElement.TOP;
      }
    }
  };
  
  public static class ErrorLocationDomain implements AbstractDomain {
    
    @Override
    public AbstractElement getBottomElement() {
      return ErrorLocationCPA.ErrorLocationElement.BOTTOM;
    }
    
    public AbstractElement getErrorElement() {
      return ErrorLocationCPA.ErrorLocationElement.ERROR;
    }
    
    @Override
    public JoinOperator getJoinOperator() {
      return joinOperator;
    }
    
    @Override
    public PartialOrder getPartialOrder() {
      return partialOrder;
    }
        
    @Override
    public AbstractElement getTopElement() {
      return ErrorLocationCPA.ErrorLocationElement.TOP;
    }
  };
  
  private static final ErrorLocationDomain domain = new ErrorLocationDomain();
  
  private static final PartialOrder partialOrder = new EqualityPartialOrder(domain);
  
  private static final StopOperator stopOperator = new StopSepOperator(partialOrder);
  
  private static final TransferRelation transferRelation = new ErrorLocationTransferRelation(domain);
  
  public ErrorLocationCPA(String mergeType, String stopType) { }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return ErrorLocationElement.NORMAL;
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode node) {
    return null;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisisonAdjustment.getInstance();
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