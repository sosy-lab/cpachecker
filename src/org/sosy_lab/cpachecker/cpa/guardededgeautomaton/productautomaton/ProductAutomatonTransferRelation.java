package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;

public class ProductAutomatonTransferRelation implements TransferRelation {
  
  private static final ProductAutomatonTransferRelation sInstance = new ProductAutomatonTransferRelation();
  
  public static ProductAutomatonTransferRelation getInstance() {
    return sInstance;
  }
  
  private static final ProductAutomatonUndeterminedElement sUndeterminedElement = ProductAutomatonUndeterminedElement.getInstance();
  private static final Set<ProductAutomatonUndeterminedElement> sUndeterminedElementSingleton = Collections.singleton(sUndeterminedElement);
  private static final Set<ProductAutomatonAcceptingElement> sAcceptingElementSingleton = Collections.singleton(ProductAutomatonAcceptingElement.getInstance());
  private static final Set<ProductAutomatonNonAcceptingElement> sNonAcceptingElementSingleton = Collections.singleton(ProductAutomatonNonAcceptingElement.getInstance());
  
  private ProductAutomatonTransferRelation() {
    
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    return sUndeterminedElementSingleton;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    if (pElement.equals(sUndeterminedElement)) {
      for (AbstractElement lOtherElement : pOtherElements) {
        if (lOtherElement instanceof GuardedEdgeAutomatonStateElement) {
          GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lOtherElement;
          
          if (!lStateElement.isFinalState()) {
            return sNonAcceptingElementSingleton;
          }
        }
      }
      
      return sAcceptingElementSingleton;
    }
    else {
      return null;
    }
  }

}
