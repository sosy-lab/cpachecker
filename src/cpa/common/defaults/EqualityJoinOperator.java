/**
 * 
 */
package cpa.common.defaults;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;

/**
 * @author holzera
 *
 * This class implements a join operator according to the partial order
 * implemented in EqualityPartialOrder (flat lattice).
 */
public class EqualityJoinOperator implements JoinOperator {

  private final PartialOrder mPartialOrder;
  private final AbstractElement mTopElement;
  
  public EqualityJoinOperator(AbstractDomain pDomain) {
    assert(pDomain != null);
    
    this.mPartialOrder = pDomain.getPartialOrder();
    this.mTopElement = pDomain.getTopElement();
  }
  
  @Override
  public AbstractElement join(AbstractElement element1, AbstractElement element2) throws CPAException {
    if (this.mPartialOrder.satisfiesPartialOrder(element1, element2)) {
      return element2;
    }
    
    if (this.mPartialOrder.satisfiesPartialOrder(element2, element1)) {
      return element1;
    }
    
    return this.mTopElement;
  }

}
