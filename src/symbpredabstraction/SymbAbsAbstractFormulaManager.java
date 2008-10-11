package symbpredabstraction;

import java.util.Collection;
import java.util.Deque;

import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;

/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbAbsAbstractFormulaManager extends AbstractFormulaManager {

    /**
     * Abstract post operation.
     */
    public AbstractFormula buildAbstraction(SummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ, 
            Collection<Predicate> predicates);

    /**
     * Counterexample analysis and predicate discovery.
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SummaryFormulaManager mgr, 
            Deque<SummaryAbstractElement> abstractTrace);
}
