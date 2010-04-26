/**
 * 
 */
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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
