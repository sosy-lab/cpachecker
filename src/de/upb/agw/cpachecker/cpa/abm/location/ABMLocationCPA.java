package de.upb.agw.cpachecker.cpa.abm.location;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;

import de.upb.agw.cpachecker.cpa.abm.predicate.ABMPTransferRelation;

/**
 * Implements a <code>LocationCPA</code> that replaces the normal <code>LocationTransferRelation</code> by a
 * <code>ABMLTransferRelation</code>.
 * @see de.upb.agw.cpachecker.cpa.abm.location.ABMLTransferRelation
 * @author dwonisch
 *
 */
public class ABMLocationCPA extends LocationCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ABMLocationCPA.class);
	}

  private ABMLTransferRelation transfer;
  private ABMLocationCPA() {
    super();
     transfer = new ABMLTransferRelation(elementFactory);
  }  	

  @Override
  public ABMLTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public AbstractElement getInitialElement(CFANode node) {
    elementFactory.initialize(node);    
    return elementFactory.getElement(node);
  }
  
  public LocationElement getLocationForNode(CFANode node) {
    return elementFactory.getElement(node);
  }
  
  public void setPredicateTransferRelation(ABMPTransferRelation predicateTransferRelation) {
    transfer.setPredicateTransferRelation(predicateTransferRelation);
  }
}