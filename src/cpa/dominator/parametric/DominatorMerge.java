/**
 *
 */
package cpa.dominator.parametric;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;

/**
 * @author holzera
 *
 */
public class DominatorMerge implements MergeOperator {
	private DominatorDomain domain = null;

	public DominatorMerge(DominatorDomain domain) {
		if (domain == null) {
			throw new IllegalArgumentException("domain is null!");
		}

		this.domain = domain;
	}

	/* (non-Javadoc)
	 * @see cpa.common.interfaces.MergeOperator#merge(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.AbstractElement)
	 */
	public AbstractElement merge(AbstractElement element1,
			AbstractElement element2, Precision prec) {
			AbstractElement joinedElement = this.domain.join(element1, element2);

			if (joinedElement.equals(this.domain.getTopElement())) {
				return element2;
			}
			else {
				return joinedElement;
			}
	}

	public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
	                             AbstractElementWithLocation element2, Precision prec) {
	    AbstractElementWithLocation joinedElement = this.domain.join(element1, element2);

	    if (joinedElement.equals(this.domain.getTopElement())) {
	      return element2;
	    }
	    else {
	      return joinedElement;
	    }
	}

}
