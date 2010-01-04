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

  private PartialOrder mPartialOrder = null;
  private AbstractElement mBottomElement = null;
  
  public EqualityJoinOperator(AbstractDomain pDomain) {
    assert(pDomain != null);
    
    this.mPartialOrder = pDomain.getPartialOrder();
    this.mBottomElement = pDomain.getBottomElement();
  }
  
  public AbstractElement join(AbstractElement element1, AbstractElement element2)
  {
    try {
      if (this.mPartialOrder.satisfiesPartialOrder(element1, element2)) {
        return element2;
      }
      
      if (this.mPartialOrder.satisfiesPartialOrder(element2, element1)) {
        return element1;
      }
    } catch (CPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      
      System.exit(0);
    }
    
    return this.mBottomElement;
  }

}
