/**
 *
 */
package cpa.pointsto;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToDomain implements AbstractDomain {

  private static class PointsToTopElement extends PointsToElement implements TopElement {}

  private static class PointsToBottomElement extends PointsToElement implements BottomElement {}

  private static class PointsToPartialOrder implements PartialOrder {
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2) {
      PointsToElement pointsToElement1 = (PointsToElement) element1;
      PointsToElement pointsToElement2 = (PointsToElement) element2;

      if (pointsToElement2.equals(topElement)) return true;
      if (pointsToElement1.equals(bottomElement)) return true;
      return pointsToElement1.subsetOf(pointsToElement2);
    }
  }

  private static class PointsToJoinOperator implements JoinOperator
  {
    public AbstractElement join (AbstractElement element1, AbstractElement element2)
    {
      PointsToElement joined = ((PointsToElement) element1).clone();
      joined.join((PointsToElement) element2);
      return joined;
    }
  }

  private final static BottomElement bottomElement = new PointsToBottomElement ();
  private final static TopElement topElement = new PointsToTopElement ();
  private final static PartialOrder partialOrder = new PointsToPartialOrder ();
  private final static JoinOperator joinOperator = new PointsToJoinOperator ();

  public PointsToDomain () {	}

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getBottomElement()
   */
  public AbstractElement getBottomElement() {
    return bottomElement;
  }

  public boolean isBottomElement(AbstractElement element) {
    return element.equals(bottomElement);
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getJoinOperator()
   */
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getPreOrder()
   */
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getTopElement()
   */
  public AbstractElement getTopElement() {
    return topElement;
  }

}
