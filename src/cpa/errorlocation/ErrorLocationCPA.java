package cpa.errorlocation;

import java.util.Collection;

import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
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
  
  private static final MergeOperator mergeOperator = new MergeOperator() {

    @Override
    public AbstractElement merge(AbstractElement element1,
                                 AbstractElement element2, Precision precision)
                                 throws CPAException {
      return element2;
    }

    @Override
    public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
                                             AbstractElementWithLocation element2,
                                             Precision precision)
                                             throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
  };
  
  private static final StopOperator stopOperator = new StopOperator() {

    @Override
    public <AE extends AbstractElement> boolean stop(AE element,
                                                     Collection<AE> reached,
                                                     Precision precision) throws CPAException {
      for (AbstractElement reachedElement : reached) {
        if (partialOrder.satisfiesPartialOrder (element, reachedElement)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean stop(AbstractElement element, AbstractElement reachedElement)
        throws CPAException {
      return partialOrder.satisfiesPartialOrder(element, reachedElement);
    }
  };
  
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
  
  private static final PartialOrder partialOrder = new PartialOrder() {
    
    @Override
    public boolean satisfiesPartialOrder(AbstractElement newElement,
                                         AbstractElement reachedElement) throws CPAException {
      if (newElement == reachedElement) {
        return true;
      } else if (newElement == ErrorLocationElement.BOTTOM || reachedElement == ErrorLocationElement.TOP) {
        return true;
      } else {
        return false;
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
  
  private final TransferRelation transferRelation = new ErrorLocationTransferRelation(domain);
  
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
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return null;
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