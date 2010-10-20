package org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class GuardedEdgeAutomatonTransferRelation implements TransferRelation {

  private final AbstractElement mTopElement;
  private final Automaton<GuardedEdgeLabel> mAutomaton;
  
  private final HashMap<Automaton<GuardedEdgeLabel>.Edge, GuardedEdgeAutomatonStateElement> mCache;
  
  private final HashSet<GuardedEdgeAutomatonStateElement> mSuccessors;
  
  private final Collection<Automaton.State> mReachedAutomatonStates; 
  
  public GuardedEdgeAutomatonTransferRelation(GuardedEdgeAutomatonDomain pDomain, Automaton<GuardedEdgeLabel> pAutomaton, Collection<Automaton.State> pReachedAutomatonStates) {
    mTopElement = pDomain.getTopElement();
    mAutomaton = pAutomaton;
    
    // create cache
    mCache = new HashMap<Automaton<GuardedEdgeLabel>.Edge, GuardedEdgeAutomatonStateElement>();
    
    HashMap<GuardedEdgeAutomatonStateElement, Automaton<GuardedEdgeLabel>.Edge> lTmpCache = new HashMap<GuardedEdgeAutomatonStateElement, Automaton<GuardedEdgeLabel>.Edge>(); 
    
    for (Automaton<GuardedEdgeLabel>.Edge lAutomatonEdge : pAutomaton.getEdges()) {
      GuardedEdgeAutomatonStateElement lElement = GuardedEdgeAutomatonStateElement.create(lAutomatonEdge, pAutomaton);
      
      if (lTmpCache.containsKey(lElement)) {
        lElement = mCache.get(lTmpCache.get(lElement));
      }
      else {
        lTmpCache.put(lElement, lAutomatonEdge);
      }
      
      mCache.put(lAutomatonEdge, lElement);
    }
    
    mSuccessors = new HashSet<GuardedEdgeAutomatonStateElement>();
    
    mReachedAutomatonStates = pReachedAutomatonStates;
  }
  
  protected Automaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    
    if (pElement.equals(mTopElement)) {
      return Collections.singleton(mTopElement);
    }

    if (pElement instanceof GuardedEdgeAutomatonPredicateElement) {
      throw new IllegalArgumentException();
    }
    
    GuardedEdgeAutomatonStandardElement lCurrentElement = (GuardedEdgeAutomatonStandardElement)pElement;
    
    if (mReachedAutomatonStates != null) {
      mReachedAutomatonStates.add(lCurrentElement.getAutomatonState());
    }
    
    mSuccessors.clear();
    
    for (Automaton<GuardedEdgeLabel>.Edge lOutgoingEdge : mAutomaton.getOutgoingEdges(lCurrentElement.getAutomatonState())) {
      GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();
      if (lLabel.contains(pCfaEdge)) {
        mSuccessors.add(mCache.get(lOutgoingEdge));
      }
    }
    
    /*if (mSuccessors.size() == 0) {
      System.out.println(pCfaEdge);
    }*/
    
    return mSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    if (pElement instanceof GuardedEdgeAutomatonPredicateElement) {
      return Collections.singleton(((GuardedEdgeAutomatonPredicateElement)pElement).getStandardElement());
    }
    
    return null;
  }

}
