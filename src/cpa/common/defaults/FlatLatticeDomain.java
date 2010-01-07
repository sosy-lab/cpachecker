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
  private final AbstractElement mTopElement;
  private final AbstractElement mBottomElement;
  private final JoinOperator mJoinOperator;
  private final PartialOrder mPartialOrder;
  
  private static class BottomElement implements AbstractElement {
    @Override
    public boolean isError() {
      return false;
    }
    
    @Override
    public String toString() {
      return "<BOTTOM>";
    }
  }
  
  private static class TopElement implements AbstractElement {
    @Override
    public boolean isError() {
      return false;
    }
    
    @Override
    public String toString() {
      return "<TOP>";
    }
  }
  
  public FlatLatticeDomain(AbstractElement pTopElement, AbstractElement pBottomElement) {
    assert(pTopElement != null);
    assert(pBottomElement != null);
    
    this.mTopElement = pTopElement;
    this.mBottomElement = pBottomElement;
    
    this.mPartialOrder = new EqualityPartialOrder(this);
    this.mJoinOperator = new EqualityJoinOperator(this);
  }
  
  public FlatLatticeDomain() {
    this(new TopElement(), new BottomElement());
  }
  
  @Override
  public AbstractElement getBottomElement() {
    return this.mBottomElement;
  }

  @Override
  public AbstractElement getTopElement() {
    return this.mTopElement;
  }

  @Override
  public JoinOperator getJoinOperator() {
    return this.mJoinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return this.mPartialOrder;
  }

}
