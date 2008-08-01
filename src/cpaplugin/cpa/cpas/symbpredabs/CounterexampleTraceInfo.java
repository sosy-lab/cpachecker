package cpaplugin.cpa.cpas.symbpredabs;

/**
 * An class that stores information about a counterexample trace. For 
 * real counterexamples, this stores the actual execution trace leading to
 * the error. For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model
 * @author alb
 */
public class CounterexampleTraceInfo {
    private boolean spurious;
    private PredicateMap pmap;
    private ConcreteTrace ctrace;
    
    public CounterexampleTraceInfo(boolean spurious) {
        this.spurious = spurious;
        pmap = null;
        ctrace = null;
    }
    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }
    
    /**
     * returns a PredicateMap with a sufficient set of predicates to refine
     * the abstract model such that this trace is no longer feasible in it
     * @return a predicate map to refine the abstraction
     */
    public PredicateMap getPredicatesForRefinement() { return pmap; }
    
    public void setPredicateMap(PredicateMap pmap) {
        this.pmap = pmap;
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
