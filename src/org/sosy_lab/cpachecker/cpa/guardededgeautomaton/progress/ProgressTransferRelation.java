package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonDomain;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStandardElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ProgressTransferRelation implements TransferRelation {

private final NondeterministicFiniteAutomaton<GuardedEdgeLabel> mAutomaton;
  
  private final HashMap<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge, GuardedEdgeAutomatonStateElement> mCache;
  
  private final HashSet<ProgressElement> mSuccessors;
  
  private final Collection<NondeterministicFiniteAutomaton.State> mReachedAutomatonStates; 
  
  public ProgressTransferRelation(GuardedEdgeAutomatonDomain pDomain, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, Collection<NondeterministicFiniteAutomaton.State> pReachedAutomatonStates) {
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
    
    mSuccessors = new HashSet<ProgressElement>();
    
    mReachedAutomatonStates = pReachedAutomatonStates;
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
    
    if (mReachedAutomatonStates != null) {
      mReachedAutomatonStates.add(lCurrentElement.getAutomatonState());
    }
    
    mSuccessors.clear();
    
    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : mAutomaton.getOutgoingEdges(lCurrentElement.getAutomatonState())) {
      GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();
      if (lLabel.contains(pCfaEdge)) {
        ProgressElement lWrappedSuccessor = new ProgressElement(mCache.get(lOutgoingEdge), lOutgoingEdge);
        mSuccessors.add(lWrappedSuccessor);
      }
    }
    
    return mSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    
    ProgressElement lWrapperElement = (ProgressElement)pElement;
    
    if (lWrapperElement.getWrappedElement() instanceof GuardedEdgeAutomatonPredicateElement) {
      return Collections.singleton(((GuardedEdgeAutomatonPredicateElement)lWrapperElement.getWrappedElement()).getStandardElement());
    }
    
    return null;
  }

}
