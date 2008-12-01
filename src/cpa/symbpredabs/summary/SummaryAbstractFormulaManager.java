package cpa.symbpredabs.summary;

import java.util.Collection;
import java.util.Deque;

import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;

/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SummaryAbstractFormulaManager extends AbstractFormulaManager {

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
