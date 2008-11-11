package symbpredabstraction;

import java.util.Collection;
import java.util.Deque;
import cpaplugin.cpa.cpas.symbpredabsCPA.SymbPredAbsAbstractElement;

public interface SymbPredAbsAbstractFormulaManager extends AbstractFormulaManager {

	/**
	 * Abstract post operation.
	 */

	public AbstractFormula buildAbstraction(SymbPredAbsFormulaManager mgr,
			SymbPredAbsAbstractElement elem, SymbPredAbsAbstractElement newElem, 
			Collection<Predicate> predicates);

	/**
	 * Counterexample analysis and predicate discovery.
	 */
	public CounterexampleTraceInfo buildCounterexampleTrace(
			SymbPredAbsFormulaManager mgr, 
			Deque<SymbPredAbsAbstractElement> abstractTrace);
}
