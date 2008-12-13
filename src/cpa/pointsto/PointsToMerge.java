/**
 *
 */
package cpa.pointsto;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import exceptions.CPAException;

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
	 * @see cpa.common.interfaces.MergeOperator#merge(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.AbstractElement)
	 */
	public AbstractElement merge(AbstractElement element1, AbstractElement element2) throws CPAException {
		return abstractDomain.getJoinOperator().join(element1, element2);
	}

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
