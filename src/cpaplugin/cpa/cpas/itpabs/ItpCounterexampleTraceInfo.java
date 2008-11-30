package cpaplugin.cpa.cpas.itpabs;

import java.util.HashMap;
import java.util.Map;

import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.cpas.symbpredabs.ConcreteTrace;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.logging.LazyLogger;


/**
 * An class that stores information about a counterexample trace. For 
 * real counterexamples, this stores the actual execution trace leading to
 * the error. For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpCounterexampleTraceInfo {
    private boolean spurious;
    private Map<AbstractElement, SymbolicFormula> refmap;
    private ConcreteTrace ctrace;
    
    public ItpCounterexampleTraceInfo(boolean spurious) {
        this.spurious = spurious;
        refmap = new HashMap<AbstractElement, SymbolicFormula>();
        ctrace = null;
    }
    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }
    
    public SymbolicFormula getFormulaForRefinement(AbstractElement e) {
        if (refmap.containsKey(e)) {
            return refmap.get(e);
        } else {
            return null;
        }
    }
    
    public void setFormulaForRefinement(AbstractElement e, SymbolicFormula f) {
        LazyLogger.log(LazyLogger.DEBUG_3, "SETTING REFINEMENT FOR ", e,
                ": ", f);
        refmap.put(e, f);
    }
    
    /**
     * for real counterexamples, returns the concrete execution trace leading
     * to the error
     * @return a ConcreteTrace from the entry point of the program to an error
     *         location
     */
    public ConcreteTrace getConcreteTrace() { return ctrace; }
    
    public void setConcreteTrace(ConcreteTrace ctrace) {
        this.ctrace = ctrace;
    }
}
