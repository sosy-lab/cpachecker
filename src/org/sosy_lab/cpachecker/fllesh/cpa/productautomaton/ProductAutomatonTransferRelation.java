package org.sosy_lab.cpachecker.fllesh.cpa.productautomaton;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;

public class ProductAutomatonTransferRelation implements TransferRelation {
  
  private static final ProductAutomatonTransferRelation sInstance = new ProductAutomatonTransferRelation();
  
  public static ProductAutomatonTransferRelation getInstance() {
    return sInstance;
  }
  
  private ProductAutomatonTransferRelation() {
    
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    if (pElement.equals(ProductAutomatonTopElement.getInstance())) {
      return Collections.singleton(ProductAutomatonTopElement.getInstance());
    }
    
    if (pElement.equals(ProductAutomatonBottomElement.getInstance())) {
      return Collections.emptySet();
    }
    
    return Collections.singleton(ProductAutomatonUndeterminedElement.getInstance());
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    if (pElement.equals(ProductAutomatonUndeterminedElement.getInstance())) {
      boolean lAccept = true;
      
      for (AbstractElement lOtherElement : pOtherElements) {
        if (lOtherElement instanceof GuardedEdgeAutomatonStateElement) {
          GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lOtherElement;
          
          if (!lStateElement.isFinalState()) {
            lAccept = false;
            break;
          }
        }
      }
      
      if (lAccept) {
        return Collections.singleton(ProductAutomatonAcceptingElement.getInstance());
      }
      else {
        return Collections.singleton(ProductAutomatonNonAcceptingElement.getInstance());
      }
    }
    else {
      return null;
    }
  }

}
