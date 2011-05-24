package de.upb.agw.cpachecker.cpa.abm.location;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.location.LocationElement.LocationElementFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import de.upb.agw.cpachecker.cpa.abm.predicate.ABMPTransferRelation;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;

/**
 * Represents a <code>LocationTransferRelation</code> that takes the (virtual) summary edges into account
 * by jumping from an input location of block directly to its return location.
 * @author dwonisch
 *
 */
public class ABMLTransferRelation extends LocationTransferRelation {
  
  private final LocationElementFactory factory;  
  private ABMPTransferRelation predicateTransfer; 
  
  public ABMLTransferRelation(LocationElementFactory factory) {
    super(factory);
    this.factory = factory;
  }
  
  protected void setPredicateTransferRelation(ABMPTransferRelation predicateTransferRelation) {
    this.predicateTransfer = predicateTransferRelation;
  }
  
  @Override
  public Collection<LocationElement> getAbstractSuccessors (AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException
  {
    assert cfaEdge != null : "Supplied CFAEdge must not be null.";
    CFANode node = cfaEdge.getPredecessor();
    if(predicateTransfer.isNewCallNode(node)) {
      if(node instanceof CFAFunctionDefinitionNode && node.getFunctionName().equalsIgnoreCase("main")) {
        return super.getAbstractSuccessors(element, prec, cfaEdge);
      }
      CachedSubtree subtree = predicateTransfer.getCachedSubtreeManager().getCachedSubtreeForCallNode(node);
      Set<CFANode> returnNodes = subtree.getReturnNodes();
      if(returnNodes.size() != 1) {
        throw new UnsupportedOperationException("Currently only singleton return node sets are supported.");
      }      
      return Collections.singleton(factory.getElement(returnNodes.iterator().next()));      
    } else {
      return super.getAbstractSuccessors(element, prec, cfaEdge);
   }
  }
}
