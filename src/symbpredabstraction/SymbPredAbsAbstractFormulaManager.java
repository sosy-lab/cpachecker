package symbpredabstraction;

import java.util.Collection;
import java.util.Deque;

/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbPredAbsAbstractFormulaManager extends AbstractFormulaManager {

    /**
     * Abstract post operation.
     */
	// TODO later
//    public AbstractFormula buildAbstraction(SummaryFormulaManager mgr,
//            SymbPredAbsAbstractElement e, SymbPredAbsAbstractElement succ, 
//            Collection<Predicate> predicates);
//
//    /**
//     * Counterexample analysis and predicate discovery.
//     */
//    public CounterexampleTraceInfo buildCounterexampleTrace(
//            SummaryFormulaManager mgr, 
//            Deque<SymbPredAbsAbstractElement> abstractTrace);
}
