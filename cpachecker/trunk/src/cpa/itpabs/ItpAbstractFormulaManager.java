package cpa.itpabs;

import java.util.Collection;
import java.util.Deque;

import cfa.objectmodel.CFAEdge;

import cpa.itpabs.ItpAbstractElement;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;

/**
 * An abstract formula manager for interpolation-based lazy abstraction.
 *
 * TODO - probably these two methods here should be moved to
 * AbstractFormulaManager, since pretty much every analysis that uses
 * AbstractFormulaManager re-defines them
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface ItpAbstractFormulaManager extends AbstractFormulaManager {

    public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
            ItpAbstractElement e, ItpAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates);

    public CounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<ItpAbstractElement> abstractTrace);

}
