package symbpredabstraction;

import java.util.Collection;
import java.util.Deque;

import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;

/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbPredAbstFormulaManager extends AbstractFormulaManager {

    /**
     * Abstract post operation.
     */
    public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
            AbstractFormula abs, PathFormula pathFormula,
            Collection<Predicate> predicates, MathsatSymbolicFormula functionExitFormula);

    /**
     * Counterexample analysis and predicate discovery.
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<SymbPredAbsAbstractElement> abstractTrace);
}
