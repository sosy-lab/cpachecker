package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeTransferRelation;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStandardElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.ImmutableList;

public class ProductAutomatonTransferRelation extends CompositeTransferRelation {

  public ProductAutomatonTransferRelation(
      ImmutableList<TransferRelation> pTransferRelations) {
    super(pTransferRelations);
  }

  /*
   * (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.composite.CompositeTransferRelation#getAbstractSuccessors(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge)
   * 
   * We do not strengthen!
   */
  @Override
  public Collection<CompositeElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    CompositeElement lCompositeElement = (CompositeElement)pElement;
    CompositePrecision lCompositePrecision = (CompositePrecision)pPrecision;
    
    int resultCount = 1;
    List<AbstractElement> componentElements = lCompositeElement.getElements();
    List<Collection<? extends AbstractElement>> allComponentsSuccessors = new ArrayList<Collection<? extends AbstractElement>>(size);

    for (int i = 0; i < size; i++) {
      TransferRelation lCurrentTransfer = transferRelations.get(i);
      AbstractElement lCurrentElement = componentElements.get(i);
      Precision lCurrentPrecision = lCompositePrecision.get(i);

      Collection<? extends AbstractElement> componentSuccessors = lCurrentTransfer.getAbstractSuccessors(lCurrentElement, lCurrentPrecision, pCfaEdge);
      resultCount *= componentSuccessors.size();
      
      if (resultCount == 0) {
        // shortcut
        break;
      }
      
      allComponentsSuccessors.add(componentSuccessors);
    }

    // create cartesian product of all elements we got
    Collection<List<AbstractElement>> allResultingElements
        = createCartesianProduct(allComponentsSuccessors, resultCount);
    
    Collection<CompositeElement> lSuccessors = new LinkedHashSet<CompositeElement>();
    
    for (List<AbstractElement> lSuccessor : allResultingElements) {
      ProductAutomatonElement lNewSuccessor = ProductAutomatonElement.createElement(lSuccessor);
      
      lSuccessors.add(lNewSuccessor);
    }
    
    return lSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {
    
    if (pElement instanceof ProductAutomatonElement.PredicateElement) {
      ProductAutomatonElement.PredicateElement lElement = (ProductAutomatonElement.PredicateElement)pElement;
      List<AbstractElement> lSubelements = new ArrayList<AbstractElement>(lElement.getNumberofElements());
      
      for (AbstractElement lSubelement : lElement.getElements()) {
        if (lSubelement instanceof GuardedEdgeAutomatonPredicateElement) {
          GuardedEdgeAutomatonPredicateElement lPredicateElement = (GuardedEdgeAutomatonPredicateElement)lSubelement;
          lSubelements.add(lPredicateElement.getStandardElement());
        }
        else if (lSubelement instanceof GuardedEdgeAutomatonStandardElement) {
          lSubelements.add(lSubelement);
        }
        else {
          throw new RuntimeException("Unsupported element type!");
        }
      }
      
      return Collections.singleton(new ProductAutomatonElement.StateElement(lSubelements));
    }
    
    return null;
  }

}
