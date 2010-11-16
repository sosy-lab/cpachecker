package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.composite;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;

public abstract class ProductAutomatonElement extends CompositeElement {

  public static ProductAutomatonElement createElement(List<AbstractElement> pElements) {
    List<ECPPredicate> lPredicates = new LinkedList<ECPPredicate>();
    
    for (AbstractElement lElement : pElements) {
      if (lElement instanceof GuardedEdgeAutomatonPredicateElement) {
        GuardedEdgeAutomatonPredicateElement lPredicateElement = (GuardedEdgeAutomatonPredicateElement)lElement;
        
        for (ECPPredicate lPredicate : lPredicateElement) {
          lPredicates.add(lPredicate);
        }
      }
    }
    
    if (lPredicates.isEmpty()) {
      return new StateElement(pElements);
    }
    else {
      return new PredicateElement(pElements, lPredicates);
    }
  }
  
  public static class StateElement extends ProductAutomatonElement {

    public StateElement(List<AbstractElement> pElements) {
      super(pElements);
    }
    
  }
  
  public static class PredicateElement extends ProductAutomatonElement {
    
    private final List<ECPPredicate> mPredicates;
    
    public PredicateElement(List<AbstractElement> pElements, List<ECPPredicate> pPredicates) {
      super(pElements);
      
      mPredicates = pPredicates;
    }
    
    public List<ECPPredicate> getPredicates() {
      return mPredicates;
    }
    
    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      
      if (pOther == null) {
        return false;
      }
      
      if (getClass().equals(pOther.getClass())) {
        PredicateElement lOther = (PredicateElement)pOther;
        
        return (super.equals(lOther) && mPredicates.equals(lOther.mPredicates));
      }
      
      return false;
    }
    
  }
  
  public ProductAutomatonElement(List<AbstractElement> pElements) {
    super(pElements);
  }
  
  @Override
  public boolean isTarget() {
    if (super.getNumberofElements() == 0) {
      return false;
    }
    
    for (AbstractElement lElement : getElements()) {
      GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lElement;
      
      if (!lStateElement.isFinalState()) {
        return false;
      }
    }
    
    return true;
  }

}
