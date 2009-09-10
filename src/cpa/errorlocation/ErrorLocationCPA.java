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

  private static class ErrorLocationElement implements AbstractElement {
    @Override
    public String toString() {
      return "[]";
    }
  }
  
  private static final ErrorLocationElement initialElement = new ErrorLocationElement();
  
  private static final ErrorLocationElement bottomElement = new ErrorLocationElement() {
    
    @Override
    public String toString() {
      return "<PointerAnalysis BOTTOM>";
    }
  };
  
private static final ErrorLocationElement topElement = new ErrorLocationElement() {
    
    @Override
    public String toString() {
      return "<PointerAnalysis TOP>";
    }
  };
  
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
      return element1;
    }
  };
  
  private static final PartialOrder partialOrder = new PartialOrder() {
    
    @Override
    public boolean satisfiesPartialOrder(AbstractElement newElement,
                                         AbstractElement reachedElement) throws CPAException {
      if (newElement == bottomElement || reachedElement == topElement) {
        return true;
      } else if (reachedElement == bottomElement) {
        assert false : "Bottom element should never be in the reached set";
        return false;
      } else if (newElement == topElement) {
        return false;
      }
      return false;
    }
  };
  
  private static final AbstractDomain domain = new AbstractDomain() {
    
    @Override
    public AbstractElement getBottomElement() {
      return bottomElement;
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
      return topElement;
    }
  };
  
  private final TransferRelation transferRelation = new ErrorLocationTransferRelation(domain);
  
  public ErrorLocationCPA(String mergeType, String stopType) { }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public AbstractElement getInitialElement(
      CFAFunctionDefinitionNode node) {
    return initialElement;
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