/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.PredicateMap;

import cmdline.CPAMain;

import cfa.objectmodel.CFANode;

/**
 * A predicate map which can be updated (refined) during execution
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class UpdateablePredicateMap implements PredicateMap {

    private final Map<CFANode, Set<Predicate>> repr;
    private final Map<String, Set<Predicate>> functionGlobalPreds;
    private final Collection<Predicate> initialGlobalPreds;
    
    private final boolean globalPredicates;

    public UpdateablePredicateMap(Collection<Predicate> initial) {
        globalPredicates = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.refinement.addPredicatesGlobally");
        repr = new HashMap<CFANode, Set<Predicate>>();
        functionGlobalPreds = new HashMap<String, Set<Predicate>>();
        
        if (initial == null || initial.size() == 0) {
          initialGlobalPreds = Collections.emptySet();
        } else {
          initialGlobalPreds = Collections.unmodifiableCollection(initial);
        }
    }

    public UpdateablePredicateMap() {
        this(null);
    }

    public boolean update(CFANode n, Collection<Predicate> preds) {
        if (initialGlobalPreds.containsAll(preds)) {
          return false;
        }
      
        boolean added = false;
        if (globalPredicates) {
            String fn = n.getFunctionName();
            assert(fn != null);
            if (!functionGlobalPreds.containsKey(fn)) {
                Set<Predicate> s = new HashSet<Predicate>(initialGlobalPreds);
                functionGlobalPreds.put(fn, s);
            }
            Set<Predicate> s = functionGlobalPreds.get(fn);
            added |= s.addAll(preds);
            if (added) {
              CPAMain.logManager.log(Level.ALL, "Updated predicates for function", fn, ":", s);
            }
        } else {
            if (!repr.containsKey(n)) {
                Set<Predicate> s = new HashSet<Predicate>(initialGlobalPreds);
                repr.put(n, s);
            }
            Set<Predicate> s = repr.get(n);
            added |= s.addAll(preds);
            if (added) {
              CPAMain.logManager.log(Level.ALL, "Added predicates to", n, ":", preds);
            }
        }
        return added;
    }

    @Override
    public Collection<Predicate> getRelevantPredicates(CFANode n) {
        if (globalPredicates) {
            String fn = n.getFunctionName();
            if (functionGlobalPreds.containsKey(fn)) {
                return functionGlobalPreds.get(fn);
            } else {
                return initialGlobalPreds;
            }
        } else {
            if (repr.containsKey(n)) {
                return repr.get(n);
            } else {
              return initialGlobalPreds;
            }
        }
    }

    @Override
    public Collection<Predicate> getRelevantPredicates(String fn) {
        if (functionGlobalPreds.containsKey(fn)) {
            return functionGlobalPreds.get(fn);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<CFANode> getKnownLocations() {
        return repr.keySet();
    }

    @Override
    public Collection<String> getKnownFunctions() {
        return functionGlobalPreds.keySet();
    }
    
    @Override
    public String toString() {
        if (globalPredicates) {
          return functionGlobalPreds.toString();
        } else {
          return repr.toString();
        }
    }
}
