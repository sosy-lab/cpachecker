package cpaplugin.cpa.cpas.predicateabstraction;

import java.util.Collection;

import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.PartialOrder;
import cpaplugin.cpa.common.interfaces.StopOperator;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class PredicateAbstractionStopSep implements StopOperator
{
	private PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionStopSep (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public boolean stop (AbstractElement element, Collection<AbstractElement> reached) throws CPAException
	{
		PartialOrder preOrder = predicateAbstractionDomain.getPreOrder ();
		for (AbstractElement testElement : reached)
		{
			CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, " Preorder check: element:  " + element
					+ " reached " + reached + " --> "+ preOrder.satisfiesPreOrder (element, testElement));
			if (preOrder.satisfiesPreOrder (element, testElement))
				return true;
		}

		return false;
	}

	public boolean isBottomElement(AbstractElement element) {

		PredicateAbstractionElement predAbsElem = (PredicateAbstractionElement) element;

		if(predAbsElem.equals(predicateAbstractionDomain.getBottomElement())){
			return true;
		}

		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		PartialOrder preOrder = predicateAbstractionDomain.getPreOrder ();
		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, " Preorder check: element:  " + element
				+ " reached element" + reachedElement + " --> "+ preOrder.satisfiesPreOrder (element, reachedElement));
		if (preOrder.satisfiesPreOrder (element, reachedElement))
			return true;
		return false;
	}
}
