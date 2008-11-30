package symbpredabstraction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.logging.LazyLogger;


/**
 * A predicate map which can be updated (refined) during execution
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class UpdateablePredicateMap implements PredicateMap {
    
    private Map<CFANode, Set<Predicate>> repr;
    private Map<String, Set<Predicate>> functionGlobalPreds;
    private Collection<Predicate> initialGlobalPreds;

    public UpdateablePredicateMap(Collection<Predicate> initial) {
        repr = new HashMap<CFANode, Set<Predicate>>();
        functionGlobalPreds = new HashMap<String, Set<Predicate>>();
        initialGlobalPreds = initial;
    }
    
    public UpdateablePredicateMap() {
        this(null);
    }
    
    public boolean update(CFANode n, Collection<Predicate> preds) {
        boolean added = false;
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.refinement.addPredicatesGlobally")) {
            String fn = n.getFunctionName();
            assert(fn != null);
            if (!functionGlobalPreds.containsKey(fn)) {
                Set<Predicate> s = new HashSet<Predicate>();
                if (initialGlobalPreds != null) {
                    added |= s.addAll(initialGlobalPreds);
                }
                functionGlobalPreds.put(fn, s);
            }
            Set<Predicate> s = functionGlobalPreds.get(fn);
            added |= s.addAll(preds);
            if (added) {
                LazyLogger.log(LazyLogger.DEBUG_1, 
                        "UPDATED PREDICATES FOR FUNCTION ", fn, ": ", s);
            }
        } else {
            if (!repr.containsKey(n)) {
                Set<Predicate> s = new HashSet<Predicate>();
                if (initialGlobalPreds != null) {
                    added |= s.addAll(initialGlobalPreds);
                }
                repr.put(n, s);
            }
            Set<Predicate> s = repr.get(n);
            added |= s.addAll(preds);
            if (added) {
                LazyLogger.log(LazyLogger.DEBUG_1, "UPDATED PREDICATES FOR ", n,
                        ": ", s);
            }
        }
        return added;
    }

    @Override
    public Collection<Predicate> getRelevantPredicates(CFANode n) {
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.refinement.addPredicatesGlobally")) {
            String fn = n.getFunctionName();
            if (functionGlobalPreds.containsKey(fn)) {
                return functionGlobalPreds.get(fn);
            } else if (initialGlobalPreds != null) {
                Set<Predicate> s = new HashSet<Predicate>();
                s.addAll(initialGlobalPreds);
                functionGlobalPreds.put(fn, s);
                return s;
            } else {
                return Collections.emptySet();
            }
        } else {
            if (repr.containsKey(n)) {
                return repr.get(n);
            } else if (initialGlobalPreds != null) {
                Set<Predicate> s = new HashSet<Predicate>();
                s.addAll(initialGlobalPreds);
                repr.put(n, s);
                return s;
            } else {
                return Collections.emptySet();
            }
        }
    }
    
    // TODO fix
    public Collection<Predicate> getRelevantPredicates() {
        return null;
    }
    
    public Collection<Predicate> getRelevantPredicates(String fn) {
        if (functionGlobalPreds.containsKey(fn)) {
            return functionGlobalPreds.get(fn);
        } else {
            return Collections.emptySet();
        }
    }
    
    public Collection<CFANode> getKnownLocations() {
        return repr.keySet();
    }

    public Collection<String> getKnownFunctions() {
        return functionGlobalPreds.keySet();
    }
}
