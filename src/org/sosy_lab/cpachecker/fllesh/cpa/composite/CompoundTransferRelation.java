package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CompoundTransferRelation implements TransferRelation {

  private TransferRelation[] mTransferRelations;
  private AbstractDomain[] mDomains;
  
  public CompoundTransferRelation(List<TransferRelation> pTransferRelations, List<AbstractDomain> pDomains) {
    
    mTransferRelations = new TransferRelation[pTransferRelations.size()];
    for (int lIndex = 0; lIndex < mTransferRelations.length; lIndex++) {
      mTransferRelations[lIndex] = pTransferRelations.get(lIndex);
    }
    
    mDomains = new AbstractDomain[mTransferRelations.length];
    for (int lIndex = 0; lIndex < mTransferRelations.length; lIndex++) {
      mDomains[lIndex] = pDomains.get(lIndex);
    }
    
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    /** 1) Determine successors */
    List<Collection<? extends AbstractElement>> lSuccessorElements = new ArrayList<Collection<? extends AbstractElement>>();
    
    CompoundElement lCompositeElement = (CompoundElement)pElement;
    CompositePrecision lCompositePrecision = (CompositePrecision)pPrecision;
    
    int lNumberOfSuccessors = 1;
    
    for (int lIndex = 0; lIndex < mTransferRelations.length; lIndex++) {
      Collection<? extends AbstractElement> lSuccessors = mTransferRelations[lIndex].getAbstractSuccessors(lCompositeElement.getSubelement(lIndex), lCompositePrecision.get(lIndex), pCfaEdge); 
      lSuccessorElements.add(lSuccessors);
      lNumberOfSuccessors *= lSuccessors.size();
    }
    
    /** 2) Cartesian product */
    List<AbstractElement> lPrefix = new ArrayList<AbstractElement>(mTransferRelations.length);
    HashSet<List<AbstractElement>> lAllSuccessors = new HashSet<List<AbstractElement>>(lNumberOfSuccessors);
    createCartesianProduct(lSuccessorElements, lPrefix, lAllSuccessors);
    
    /** 3) Strengthening */
    HashSet<CompoundElement> lSuccessors = new HashSet<CompoundElement>();
    
    for (List<AbstractElement> lReachedElement : lAllSuccessors) {
      
      List<Collection<? extends AbstractElement>> lStrengthenedElements = new ArrayList<Collection<? extends AbstractElement>>(mTransferRelations.length);
      
      int lNumberOfResultingElements = 1;
      
      for (int lIndex = 0; lIndex < mTransferRelations.length && lNumberOfResultingElements > 0; lIndex++) {
        
        AbstractElement lCurrentElement = lReachedElement.get(lIndex);
        TransferRelation lCurrentTransferRelation = mTransferRelations[lIndex];
        
        Collection<? extends AbstractElement> lResultsList = lCurrentTransferRelation.strengthen(lCurrentElement, lReachedElement, pCfaEdge, (lCompositePrecision == null) ? null : lCompositePrecision.get(lIndex));

        if (lResultsList == null) {
          lStrengthenedElements.add(Collections.singleton(lCurrentElement));
        }
        else {
          lNumberOfResultingElements *= lResultsList.size();
          lStrengthenedElements.add(lResultsList);
        }
      }
      
      if (lNumberOfResultingElements > 0) {
        Collection<List<AbstractElement>> lResultingElements = new ArrayList<List<AbstractElement>>(lNumberOfResultingElements);
        List<AbstractElement> lInitialPrefix = Collections.emptyList();
        createCartesianProduct(lStrengthenedElements, lInitialPrefix, lResultingElements);
        
        for (List<AbstractElement> lList : lResultingElements) {
          lSuccessors.add(new CompoundElement(lList));
        }
      }
    }
    
    return lSuccessors;
  }

  private void createCartesianProduct(List<Collection<? extends AbstractElement>> allComponentsSuccessors,
      List<AbstractElement> prefix, Collection<List<AbstractElement>> allResultingElements) {

    if (prefix.size() == allComponentsSuccessors.size()) {
      allResultingElements.add(prefix);

    } else {
      int depth = prefix.size();
      Collection<? extends AbstractElement> myComponentsSuccessors = allComponentsSuccessors.get(depth);

      for (AbstractElement currentComponent : myComponentsSuccessors) {
        // we do not generate compound bottom elements
        if (mDomains[depth].getBottomElement().equals(currentComponent)) {
          continue;
        }
        
        List<AbstractElement> newPrefix = new ArrayList<AbstractElement>(prefix);
        
        newPrefix.add(currentComponent);

        createCartesianProduct(allComponentsSuccessors, newPrefix, allResultingElements);
      }
    }
  }
  
  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    return null;
  }

}
