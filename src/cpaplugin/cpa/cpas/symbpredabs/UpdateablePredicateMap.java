package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;

public class UpdateablePredicateMap implements PredicateMap {
    
    private Map<CFANode, Set<Predicate>> repr;

    public UpdateablePredicateMap() {
        repr = new HashMap<CFANode, Set<Predicate>>();
    }
    
    public boolean update(CFANode n, Collection<Predicate> preds) {
        if (!repr.containsKey(n)) {
            repr.put(n, new HashSet<Predicate>());
        }
        Set<Predicate> s = repr.get(n);
        boolean added = s.addAll(preds);
        if (added) {
            LazyLogger.log(LazyLogger.DEBUG_1, "UPDATED PREDICATES FOR ", n,
                           ": ", s);
        }
        return added;
    }

    @Override
    public Collection<Predicate> getRelevantPredicates(CFANode n) {
        if (repr.containsKey(n)) {
            return repr.get(n);
        } else {
            return Collections.emptySet();
        }
    }

}
