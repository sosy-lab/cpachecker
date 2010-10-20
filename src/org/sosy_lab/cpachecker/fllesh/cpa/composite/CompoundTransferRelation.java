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

  private final TransferRelation[] mTransferRelations;
  private final AbstractDomain[] mDomains;
  
  private final List<Collection<? extends AbstractElement>> mSuccessorElements;
  private final ArrayList<AbstractElement> mPrefix;
  private final HashSet<List<AbstractElement>> mAllSuccessors;
  private final List<Collection<? extends AbstractElement>> mStrengthenedElements;
  private final AbstractElement[] mInitialPrefix;
  
  private final int mNumberOfCPAs;
  
  private final HashSet<CompoundElement> lSuccessors;
    
  public CompoundTransferRelation(List<TransferRelation> pTransferRelations, List<AbstractDomain> pDomains) {
    
    mTransferRelations = new TransferRelation[pTransferRelations.size()];
    mNumberOfCPAs = mTransferRelations.length;
    mDomains = new AbstractDomain[mNumberOfCPAs];
    
    for (int lIndex = 0; lIndex < mNumberOfCPAs; lIndex++) {
      mTransferRelations[lIndex] = pTransferRelations.get(lIndex);
      mDomains[lIndex] = pDomains.get(lIndex);
    }
    
    mSuccessorElements = new ArrayList<Collection<? extends AbstractElement>>(mNumberOfCPAs);
    mPrefix = new ArrayList<AbstractElement>(mNumberOfCPAs);
    for (int lIndex = 0; lIndex < mNumberOfCPAs; lIndex++) {
      mPrefix.add(null);
    }
    
    mAllSuccessors = new HashSet<List<AbstractElement>>();
    mStrengthenedElements = new ArrayList<Collection<? extends AbstractElement>>(mNumberOfCPAs);
    
    mInitialPrefix = new AbstractElement[mNumberOfCPAs];
    
    lSuccessors = new HashSet<CompoundElement>();
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    /** 1) Determine successors */
    mSuccessorElements.clear();
    
    CompoundElement lCompositeElement = (CompoundElement)pElement;
    CompositePrecision lCompositePrecision = (CompositePrecision)pPrecision;
    
    int lNumberOfSuccessors = 1;
    
    for (int lIndex = 0; lIndex < mNumberOfCPAs; lIndex++) {
      Collection<? extends AbstractElement> lSuccessors = mTransferRelations[lIndex].getAbstractSuccessors(lCompositeElement.getSubelement(lIndex), lCompositePrecision.get(lIndex), pCfaEdge); 
      mSuccessorElements.add(lSuccessors);
      lNumberOfSuccessors *= lSuccessors.size();
      
      if (lNumberOfSuccessors == 0) {
        return Collections.emptySet();
      }
    }
    
    /** 2) Cartesian product */
    mAllSuccessors.clear();
    createCartesianProduct(0, mSuccessorElements, mPrefix, mAllSuccessors);
    
    /** 3) Strengthening */
    lSuccessors.clear();
    
    for (List<AbstractElement> lReachedElement : mAllSuccessors) {
      mStrengthenedElements.clear();
      
      int lNumberOfResultingElements = 1;
      
      for (int lIndex = 0; lIndex < mNumberOfCPAs && lNumberOfResultingElements > 0; lIndex++) {
        
        AbstractElement lCurrentElement = lReachedElement.get(lIndex);
        TransferRelation lCurrentTransferRelation = mTransferRelations[lIndex];
        
        Collection<? extends AbstractElement> lResultsList = lCurrentTransferRelation.strengthen(lCurrentElement, lReachedElement, pCfaEdge, (lCompositePrecision == null) ? null : lCompositePrecision.get(lIndex));

        if (lResultsList == null) {
          mStrengthenedElements.add(Collections.singleton(lCurrentElement));
        }
        else {
          lNumberOfResultingElements *= lResultsList.size();
          mStrengthenedElements.add(lResultsList);
        }
      }
      
      if (lNumberOfResultingElements > 0) {
        createCartesianProduct2(0, mStrengthenedElements, mInitialPrefix, lSuccessors);
      }
    }
    
    return lSuccessors;
  }

  private void createCartesianProduct(int depth, List<Collection<? extends AbstractElement>> allComponentsSuccessors,
      ArrayList<AbstractElement> prefix, Collection<List<AbstractElement>> allResultingElements) {

    if (depth == mNumberOfCPAs) {
      allResultingElements.add(new ArrayList<AbstractElement>(prefix));
    } else {
      Collection<? extends AbstractElement> myComponentsSuccessors = allComponentsSuccessors.get(depth);

      for (AbstractElement currentComponent : myComponentsSuccessors) {
        prefix.set(depth, currentComponent);
        
        createCartesianProduct(depth + 1, allComponentsSuccessors, prefix, allResultingElements);
      }
    }
  }
  
  private void createCartesianProduct2(int depth, List<Collection<? extends AbstractElement>> allComponentsSuccessors,
      AbstractElement[] lComponents, Collection<CompoundElement> allResultingElements) {

    if (depth == mNumberOfCPAs) {
      allResultingElements.add(new CompoundElement(lComponents));
    } else {
      Collection<? extends AbstractElement> myComponentsSuccessors = allComponentsSuccessors.get(depth);

      for (AbstractElement currentComponent : myComponentsSuccessors) {
        lComponents[depth] = currentComponent;
        
        createCartesianProduct2(depth + 1, allComponentsSuccessors, lComponents, allResultingElements);
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
