package cpa.observeranalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link ObserverInternalState} 
 * by evaluating the {@link ObserverTransition.match(CFAEdge)} method for 
 * all outgoing {@link ObserverTransition}s of this State.
 * @author rhein
 */
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
    ObserverState ns =((ObserverState)pElement).getFollowState(pCfaEdge); 
    return ns;
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
                      AbstractElement element, Precision precision, CFAEdge cfaEdge)
                      throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, precision));
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                                    List<AbstractElement> otherElements,
                                    CFAEdge cfaEdge, Precision precision)
                                    throws CPATransferException {
    return null;
  }
}
