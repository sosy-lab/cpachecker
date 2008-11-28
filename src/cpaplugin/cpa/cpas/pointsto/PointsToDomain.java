/**
 * 
 */
package cpaplugin.cpa.cpas.pointsto;

import java.util.Iterator;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.BottomElement;
import cpaplugin.cpa.common.interfaces.JoinOperator;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.TopElement;

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
			Iterator<PointsToRelation> iter = pointsToElement1.getIterator();
			while (iter.hasNext()) {
				if(!pointsToElement2.containsRecursive(iter.next())) return false;
			}
			
			return true;
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
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getBottomElement()
	 */
	public BottomElement getBottomElement() {
		return bottomElement;
	}
	
	public boolean isBottomElement(AbstractElement element) {
		return element.equals(bottomElement);
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getJoinOperator()
	 */
	public JoinOperator getJoinOperator() {
		return joinOperator;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getPreOrder()
	 */
	public PartialOrder getPartialOrder() {
		return partialOrder;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.AbstractDomain#getTopElement()
	 */
	public TopElement getTopElement() {
		return topElement;
	}

}
