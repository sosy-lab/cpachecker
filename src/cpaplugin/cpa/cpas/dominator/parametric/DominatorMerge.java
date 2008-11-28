/**
 * 
 */
package cpaplugin.cpa.cpas.dominator.parametric;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;
import cpaplugin.exceptions.CPAException;

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
	 * @see cpaplugin.cpa.common.interfaces.MergeOperator#getAbstractDomain()
	 */
	@Override
	public AbstractDomain getAbstractDomain() {
		return this.domain;
	}

	/* (non-Javadoc)
	 * @see cpaplugin.cpa.common.interfaces.MergeOperator#merge(cpaplugin.cpa.common.interfaces.AbstractElement, cpaplugin.cpa.common.interfaces.AbstractElement)
	 */
	@Override
	public AbstractElement merge(AbstractElement element1,
			AbstractElement element2) {
		try {
			AbstractElement joinedElement = this.domain.getJoinOperator().join(element1, element2);
			
			if (joinedElement.equals(this.domain.getTopElement())) {
				return element2;
			}
			else {
				return joinedElement;
			}
		}
		catch (CPAException e) {
			return this.domain.getTopElement();
		}
	}

}
