package de.upb.agw.cpachecker.cpa.abm.callstack;

import java.util.Collection;
import java.util.Collections;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import de.upb.agw.cpachecker.cpa.abm.predicate.ABMPTransferRelation;

/**
 * Represents a <code>CallstackTransferRelation</code> that does not create a new <code>CallstackElement</code> if a call side
 * is the input-location of a new block (the function call will be handled inside the block's CPA).
 * @author dwonisch
 *
 */
public class ABMCTransferRelation extends CallstackTransferRelation {

  private ABMPTransferRelation predicateTransfer; 
  
  protected void setPredicateTransferRelation(ABMPTransferRelation predicateTransferRelation) {
    this.predicateTransfer = predicateTransferRelation;
  }
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException
  {
    assert cfaEdge != null : "Supplied CFAEdge must not be null.";
    CFANode node = cfaEdge.getPredecessor();
    if(predicateTransfer.isNewCallNode(node)) {
      return Collections.singleton(element); 
    } else {
      return super.getAbstractSuccessors(element, prec, cfaEdge);
   }
  }
}