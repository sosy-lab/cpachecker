package cpaplugin.cpa.cpas.symbpredabs.explicit;

import java.util.Collection;
import java.util.Deque;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cpa.common.interfaces.AbstractElementWithLocation;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;

/**
 * Formula Manager for Explicit-state predicate abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface ExplicitAbstractFormulaManager extends AbstractFormulaManager {

    /**
     * Computes the abstract post from "e" to "succ" on the given edge
     * wrt. the given set of predicates.
     */
    public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
            ExplicitAbstractElement e, ExplicitAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates);

    /**
     * Counterexample analysis. If the trace is spurious, the returned object
     * will carry information about the predicates needed for
     * refinement. Otherwise, it will contain information about the concrete
     * execution path leading to the error.
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr, 
            Deque<ExplicitAbstractElement> abstractTrace);


    public class ConcretePath {
        public Vector<SymbolicFormula> path;
        public boolean theoryCombinationNeeded;
        public SSAMap ssa;
        
        public ConcretePath(Vector<SymbolicFormula> p, boolean tc, SSAMap s) {
            path = p;
            theoryCombinationNeeded = tc;
            ssa = s;
        }
    }
    
    public ConcretePath buildConcretePath(SymbolicFormulaManager mgr,
            AbstractElementWithLocation[] path) 
                throws UnrecognizedCFAEdgeException;
    
    public Vector<SymbolicFormula> getUsefulBlocks(SymbolicFormulaManager mgr,
            Vector<SymbolicFormula> trace, boolean theoryCombinationNeeded,
            boolean suffixTrace, boolean zigZag, boolean setAllTrueIfSat);
}
