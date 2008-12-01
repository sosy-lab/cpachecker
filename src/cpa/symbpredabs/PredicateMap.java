package cpa.symbpredabs;

import java.util.Collection;


import cfa.objectmodel.CFANode;


/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * A map from domain elements to a collection of predicates
 *
 */
public interface PredicateMap {
    /**
     * gets the predicates relevant to the given CFA node
     * @param n the node for which to retrieve the list of predicates
     * @return the list of relevant predicates
     */
    public Collection<Predicate> getRelevantPredicates(CFANode n);

    /**
     * gets the predicates relevant to the given function.
     * This is meaningful only if the option to add predicates globally is set
     * to true.
     */
    public Collection<Predicate> getRelevantPredicates(String functionName);

    /**
     * returns the list of all locations with some predicates attached to them.
     */
    public Collection<CFANode> getKnownLocations();

    /**
     * returns the list of all functions with some predicates attached to them.
     */
    public Collection<String> getKnownFunctions();

}
