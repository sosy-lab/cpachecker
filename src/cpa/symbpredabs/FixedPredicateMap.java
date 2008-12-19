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
package cpa.symbpredabs;

import java.util.Collection;
import java.util.Collections;


import cfa.objectmodel.CFANode;


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
