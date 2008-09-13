package symbpredabstraction;

import java.util.Collection;
import java.util.Deque;

import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabsCPA.SymbPredAbsAbstractElement;

/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbPredAbsAbstractFormulaManager extends AbstractFormulaManager {

    /**
     * Abstract post operation.
     */
    public AbstractFormula buildAbstraction(SymbPredAbsFormulaManager mgr,
    		SymbPredAbsAbstractElement e, SymbPredAbsAbstractElement succ, 
            Collection<Predicate> predicates);

    /**
     * Counterexample analysis and predicate discovery.
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
    		SymbPredAbsFormulaManager mgr, 
            Deque<SymbPredAbsAbstractElement> abstractTrace);
}
