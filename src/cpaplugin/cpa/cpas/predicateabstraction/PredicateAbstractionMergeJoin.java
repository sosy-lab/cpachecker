package cpaplugin.cpa.cpas.predicateabstraction;

import predicateabstraction.Predicate;
import predicateabstraction.ThreeValuedBoolean;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.MergeOperator;

public class PredicateAbstractionMergeJoin implements MergeOperator{

	private PredicateAbstractionDomain predicateAbstractionDomain;

	public PredicateAbstractionMergeJoin (PredicateAbstractionDomain predAbsDomain)
	{
		this.predicateAbstractionDomain = predAbsDomain;
	}

	public AbstractDomain getAbstractDomain ()
	{
		return predicateAbstractionDomain;
	}

	public AbstractElement merge (AbstractElement element1, AbstractElement element2)
	{
		PredicateAbstractionElement predAbstElement1 = (PredicateAbstractionElement) element1;
		PredicateAbstractionElement predAbstElement2 = (PredicateAbstractionElement) element2;

		if(predAbstElement1.getPredicateList().size() != 
			predAbstElement2.getPredicateList().size()){
			// TODO handle this case
		}

		System.out.println("________________________________");
		System.out.println("MERGING");
		System.out.println(predAbstElement1);
		System.out.println(predAbstElement2);
		System.out.println("________________________________");
		
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
		
		System.out.println("________________________________");
		System.out.println("MERGED");
		System.out.println(joined);
		System.out.println("________________________________");
		
		return joined;
	}
}
