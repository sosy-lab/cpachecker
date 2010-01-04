/**
 * 
 */
package cpa.common.defaults;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

/**
 * @author holzera
 *
 */
public class FlatLatticeDomain implements AbstractDomain {
  private AbstractElement mTopElement;
  private AbstractElement mBottomElement;
  private JoinOperator mJoinOperator;
  private PartialOrder mPartialOrder;
  
  public FlatLatticeDomain(AbstractElement pTopElement, AbstractElement pBottomElement) {
    assert(pTopElement != null);
    assert(pBottomElement != null);
    
    this.mTopElement = pTopElement;
    this.mBottomElement = pBottomElement;
    
    this.mPartialOrder = new EqualityPartialOrder(this);
    this.mJoinOperator = new EqualityJoinOperator(this);
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getBottomElement()
   */
  @Override
  public AbstractElement getBottomElement() {
    return this.mBottomElement;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getTopElement()
   */
  @Override
  public AbstractElement getTopElement() {
    return this.mTopElement;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getJoinOperator()
   */
  @Override
  public JoinOperator getJoinOperator() {
    return this.mJoinOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getPartialOrder()
   */
  @Override
  public PartialOrder getPartialOrder() {
    return this.mPartialOrder;
  }

}
