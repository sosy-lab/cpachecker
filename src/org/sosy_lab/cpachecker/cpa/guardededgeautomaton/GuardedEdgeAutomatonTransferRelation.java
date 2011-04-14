package org.sosy_lab.cpachecker.cpa.guardededgeautomaton;

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
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class GuardedEdgeAutomatonTransferRelation implements TransferRelation {

  private final NondeterministicFiniteAutomaton<GuardedEdgeLabel> mAutomaton;
  
  private final HashMap<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge, GuardedEdgeAutomatonStateElement> mCache;
  
  private final HashSet<GuardedEdgeAutomatonStateElement> mSuccessors;
  
  public GuardedEdgeAutomatonTransferRelation(GuardedEdgeAutomatonDomain pDomain, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    mAutomaton = pAutomaton;
    
    // create cache
    mCache = new HashMap<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge, GuardedEdgeAutomatonStateElement>();
    
    HashMap<GuardedEdgeAutomatonStateElement, NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lTmpCache = new HashMap<GuardedEdgeAutomatonStateElement, NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>(); 
    
    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lAutomatonEdge : pAutomaton.getEdges()) {
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
  }
  
  protected NondeterministicFiniteAutomaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pElement instanceof GuardedEdgeAutomatonPredicateElement) {
      throw new IllegalArgumentException();
    }
    
    GuardedEdgeAutomatonStandardElement lCurrentElement = (GuardedEdgeAutomatonStandardElement)pElement;
    
    mSuccessors.clear();
    
    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : mAutomaton.getOutgoingEdges(lCurrentElement.getAutomatonState())) {
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
