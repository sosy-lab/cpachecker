package cpa.observeranalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cfa.objectmodel.CFAEdge;
import cpa.common.LogManager;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.observeranalysis.ObserverState.ObserverUnknownState;
import exceptions.CPATransferException;

/** The TransferRelation of this CPA determines the AbstractSuccessor of a {@link ObserverState}
 * and strengthens an {@link ObserverState.ObserverUnknownState}.
 * @author rhein
 */
class ObserverTransferRelation implements TransferRelation {
  ObserverAutomaton automaton;
  LogManager logger;

  public ObserverTransferRelation(ObserverAutomaton pAutomaton, LogManager pLogger) {
    automaton = pAutomaton;
    this.logger = pLogger;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.TransferRelation#getAbstractSuccessors(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.Precision, cfa.objectmodel.CFAEdge)
   */
  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
                      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
                      throws CPATransferException {
    if (pElement instanceof ObserverUnknownState) {
      // the last CFA edge could not be processed properly 
      // (strengthen was not called on the ObserverUnknownState or the strengthen operation had not enough information to determine a new following state.) 
      return Collections.singleton((AbstractElement)ObserverState.TOP);
    }
    if (! (pElement instanceof ObserverState)) {
      throw new IllegalArgumentException("Cannot getAbstractSuccessor for non-ObserverState AbstractElements.");
    }
    AbstractElement ns =((ObserverState)pElement).getFollowState(new ObserverExpressionArguments(null, null, pCfaEdge, logger));
    return Collections.singleton(ns);
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.TransferRelation#strengthen(cpa.common.interfaces.AbstractElement, java.util.List, cfa.objectmodel.CFAEdge, cpa.common.interfaces.Precision)
   */
  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
                                    List<AbstractElement> pOtherElements,
                                    CFAEdge pCfaEdge, Precision pPrecision)
                                    throws CPATransferException {
    if (! (pElement instanceof ObserverUnknownState))
      return null;
    else {
      return ((ObserverUnknownState)pElement).strengthen(new ObserverExpressionArguments(null, pOtherElements, pCfaEdge, logger));
    }
  }
}
