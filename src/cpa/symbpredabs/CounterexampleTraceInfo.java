package cpa.symbpredabs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cpa.common.interfaces.AbstractElement;


/**
 * A class that stores information about a counterexample trace. For
 * real counterexamples, this stores the actual execution trace leading to
 * the error. For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class CounterexampleTraceInfo {
    private boolean spurious;
    private Map<AbstractElement, Set<Predicate>> pmap;
    private ConcreteTrace ctrace;

    public CounterexampleTraceInfo(boolean spurious) {
        this.spurious = spurious;
        pmap = new HashMap<AbstractElement, Set<Predicate>>();
        ctrace = null;
    }
    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }

    /**
     * returns the list of Predicates that were discovered during
     * counterexample analysis for the given AbstractElement. The invariant is
     * that the union of all the predicates for all the AbstractElements in
     * the spurious counterexample is sufficient for refining the abstract
     * model such that this trace is no longer feasible in it
     *
     * @return a list of predicates
     */
    public Collection<Predicate> getPredicatesForRefinement(AbstractElement e) {
        if (pmap.containsKey(e)) {
            return pmap.get(e);
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Adds some predicates to the list of those corresponding to the given
     * AbstractElement
     */
    public void addPredicatesForRefinement(AbstractElement e,
                                           Set<Predicate> preds) {
        if (pmap.containsKey(e)) {
            Set<Predicate> old = pmap.get(e);
            old.addAll(preds);
            pmap.put(e, old);
        } else {
            pmap.put(e, preds);
        }
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
