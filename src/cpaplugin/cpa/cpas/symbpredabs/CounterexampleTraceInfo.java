package cpaplugin.cpa.cpas.symbpredabs;

/**
 * An interface that stores information about a counterexample trace. For 
 * real counterexamples, this stores the actual execution trace leading to
 * the error. For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model
 * @author alb
 */
public interface CounterexampleTraceInfo {
    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious();
    
    /**
     * returns a PredicateMap with a sufficient set of predicates to refine
     * the abstract model such that this trace is no longer feasible in it
     * @return a predicate map to refine the abstraction
     */
    public PredicateMap getPredicatesForRefinement();
    
    /**
     * for real counterexamples, returns the concrete execution trace leading
     * to the error
     * @return a ConcreteTrace from the entry point of the program to an error
     *         location
     */
    public ConcreteTrace getConcreteTrace();
}
