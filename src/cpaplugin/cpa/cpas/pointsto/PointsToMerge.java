/**
 * 
 */
package cpaplugin.cpa.cpas.pointsto;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToMerge implements MergeOperator {
	
	private final AbstractDomain abstractDomain;
	
	public PointsToMerge (AbstractDomain abstractDomain) {
		this.abstractDomain = abstractDomain;
	}
	
	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.MergeOperator#merge(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cpa.common.interfaces.AbstractElement)
	 */
	public AbstractElement merge(AbstractElement element1, AbstractElement element2) throws CPAException {
		return abstractDomain.getJoinOperator().join(element1, element2);
	}
}
