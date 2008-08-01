package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;

import cpaplugin.cfa.objectmodel.CFANode;

/**
 * A predicate map that always returns the same set of predicates for all nodes
 * @author alb
 */
public class FixedPredicateMap implements PredicateMap {
    private Collection<Predicate> thePredicates;
    
    public FixedPredicateMap(Collection<Predicate> preds) {
        thePredicates = preds;
    }

    public Collection<Predicate> getRelevantPredicates(CFANode n) {
        return thePredicates;
    }

}
