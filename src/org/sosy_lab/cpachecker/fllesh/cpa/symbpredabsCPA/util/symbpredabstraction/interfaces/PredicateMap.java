/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces;

import java.util.Collection;


import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;


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
