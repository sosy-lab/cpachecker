package symbpredabstraction;

import java.util.Collection;
import java.util.Deque;
import cpaplugin.cpa.cpas.symbpredabsCPA.SymbPredAbsAbstractElement;

public interface SymbPredAbsAbstractFormulaManager extends AbstractFormulaManager {

	/**
	 * Abstract post operation.
	 */

	public AbstractFormula buildAbstraction(SymbolicFormulaManager symbolicFormulaManager,
			SymbPredAbsAbstractElement elem, SymbPredAbsAbstractElement newElem, 
			PredicateMap predicates);

	// TODO
//	/**
//	 * Counterexample analysis and predicate discovery.
//	 */
//	public CounterexampleTraceInfo buildCounterexampleTrace(
//			SymbPredAbsFormulaManager mgr, 
//			Deque<SymbPredAbsAbstractElement> abstractTrace);
}
