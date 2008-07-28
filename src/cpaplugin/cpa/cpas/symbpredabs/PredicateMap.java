package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;

import cpaplugin.cfa.objectmodel.CFANode;

/**
 * @author alb
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
}
