/**
 * 
 */
package cpaplugin.cpa.cpas.pointsto;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToStop implements StopOperator {

	private final AbstractDomain abstractDomain;
	
	public PointsToStop (AbstractDomain abstractDomain) {
		this.abstractDomain = abstractDomain;
	}
	
	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.StopOperator#stop(cpaplugin.cpa.common.interfaces.AbstractElement, java.util.Collection)
	 */
	public boolean stop(AbstractElement element,
			Collection<AbstractElement> reached) throws CPAException {
		for (AbstractElement r : reached) {
			if (stop(element, r)) return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.StopOperator#stop(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cpa.common.interfaces.AbstractElement)
	 */
	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
		return abstractDomain.getPartialOrder().satisfiesPartialOrder(element, reachedElement);
	}

}
