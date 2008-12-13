package cpa.dominator.parametric;

import java.util.Collection;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class DominatorStop implements StopOperator {

	private DominatorDomain domain = null;

	public DominatorStop(DominatorDomain domain) {
		if (domain == null) {
			throw new IllegalArgumentException("domain is null!");
		}

		this.domain = domain;
	}

	public <AE extends AbstractElement> boolean stop(AE element,
			Collection<AE> reached, Precision prec) throws CPAException {
		// would coverage by union of reached elements make sense here?
		for (AbstractElement reachedElement : reached)
		{
			if (stop(element, reachedElement)) {
				return true;
			}
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
			throws CPAException {
		boolean result = this.domain.getPartialOrder().satisfiesPartialOrder(element, reachedElement);

		return result;
	}

}
