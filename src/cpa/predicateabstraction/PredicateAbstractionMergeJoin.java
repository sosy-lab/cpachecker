package cpa.predicateabstraction;

import predicateabstraction.Predicate;
import predicateabstraction.ThreeValuedBoolean;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class PredicateAbstractionMergeJoin implements MergeOperator{

	private final PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionMergeJoin (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision prec)
	{
		PredicateAbstractionElement predAbstElement1 = (PredicateAbstractionElement) element1;
		PredicateAbstractionElement predAbstElement2 = (PredicateAbstractionElement) element2;

		if(predAbstElement1.getPredicateList().size() !=
			predAbstElement2.getPredicateList().size()){
			// TODO handle this case
		}

		PredicateAbstractionElement joined = new PredicateAbstractionElement(predAbstElement1.getPredicateList(), false);

		if(predAbstElement1.isFalsePredicate()){
			joined = predAbstElement2;
		}

		else if(predAbstElement2.isFalsePredicate()){
			joined = predAbstElement1;
		}

		else{
			for(Predicate pred1:predAbstElement1.getPredicateList().getPredicates()){
				Predicate pred2 = predAbstElement2.getPredicateList().getPredicate(pred1);
				Predicate joinedPred = joined.getPredicateList().getPredicate(pred1);
				if(pred1.getTruthValue() == pred2.getTruthValue()){
					joinedPred.setTruthValue(pred1.getTruthValue());
				}
				else{
					joinedPred.setTruthValue(ThreeValuedBoolean.DONTKNOW);
				}
			}
		}

		return joined;
	}

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2,
                                           Precision prec) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
