package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;
import java.util.Collections;

import cpaplugin.cfa.objectmodel.CFANode;

/**
 * A predicate map that always returns the same set of predicates for all nodes
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class FixedPredicateMap implements PredicateMap {
    private Collection<Predicate> thePredicates;
    
    public FixedPredicateMap(Collection<Predicate> preds) {
        thePredicates = preds;
    }

    public Collection<Predicate> getRelevantPredicates(CFANode n) {
        return thePredicates;
    }

    @Override
    public Collection<String> getKnownFunctions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<CFANode> getKnownLocations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Predicate> getRelevantPredicates(String functionName) {
        // TODO Auto-generated method stub
        return null;
    }

}
