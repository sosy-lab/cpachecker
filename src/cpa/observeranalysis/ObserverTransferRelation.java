package cpa.observeranalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

class ObserverTransferRelation implements TransferRelation {
  ObserverAutomaton automaton;

  public ObserverTransferRelation(ObserverAutomaton pAutomaton) {
    automaton = pAutomaton;
  }

  private AbstractElement getAbstractSuccessor(AbstractElement pElement,
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
  public Collection<AbstractElement> getAbstractSuccessors(
                      AbstractElement element, Precision precision, CFAEdge cfaEdge)
                      throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, precision));
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                                    List<AbstractElement> otherElements,
                                    CFAEdge cfaEdge, Precision precision)
                                    throws CPATransferException {
    return null;
  }
}
