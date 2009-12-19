package cpa.observeranalysis;

import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;

class ObserverTransferRelation implements TransferRelation {
  ObserverAutomaton automaton;

  public ObserverTransferRelation(ObserverAutomaton pAutomaton) {
    automaton = pAutomaton;
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement pElement,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    if (! (pElement instanceof ObserverState)) {
      throw new IllegalArgumentException("Cannot getAbstractSuccessor for non-ObserverState AbstractElements.");
    }
    
    ObserverState sourceState = (ObserverState)pElement;
    ObserverState followState = sourceState;
    
    for (ObserverTransition t : sourceState.getTransitions()) {
      if (t.match(pCfaEdge)) {
        if (t.assertionsHold()) {
          t.executeActions();
          followState = t.getFollowState();
        } else {
          followState = ObserverState.ERR;
        }
        break;
      }
    }
    if (followState!= sourceState) {
      System.out.println("Transition from " + sourceState.toString() + " to " + followState.toString());
    }
    return followState;
  }

  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
                      AbstractElementWithLocation element, Precision precision)
                      throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                                    List<AbstractElement> otherElements,
                                    CFAEdge cfaEdge, Precision precision)
                                    throws CPATransferException {
    return null;
  }
}
