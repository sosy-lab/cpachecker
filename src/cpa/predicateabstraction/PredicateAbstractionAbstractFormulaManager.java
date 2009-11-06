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
package cpa.predicateabstraction;

import java.util.Collection;
import java.util.Vector;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.trace.CounterexampleTraceInfo;

import cfa.objectmodel.CFAEdge;

import common.Pair;

import cpa.art.ARTElement;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * Formula Manager for Explicit-state predicate abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface PredicateAbstractionAbstractFormulaManager extends AbstractFormulaManager {

    /**
     * Computes the abstract post from "e" to "succ" on the given edge
     * wrt. the given set of predicates.
     */
    public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
            PredicateAbstractionAbstractElement e, PredicateAbstractionAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates);

    /**
     * Counterexample analysis. If the trace is spurious, the returned object
     * will carry information about the predicates needed for
     * refinement. Otherwise, it will contain information about the concrete
     * execution path leading to the error.
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Pair<ARTElement, CFAEdge>[] pathArray);


    public class ConcretePath {
        public Vector<SymbolicFormula> path;
        public boolean theoryCombinationNeeded;
        public SSAMap ssa;

        public ConcretePath(Vector<SymbolicFormula> p, boolean tc, SSAMap s) {
            path = p;
            theoryCombinationNeeded = tc;
            ssa = s;
        }
    }

    public ConcretePath buildConcretePath(SymbolicFormulaManager mgr,
        Pair<ARTElement, CFAEdge>[] pathArray)
                throws UnrecognizedCFAEdgeException;

    public Vector<SymbolicFormula> getUsefulBlocks(SymbolicFormulaManager mgr,
            Vector<SymbolicFormula> trace, boolean theoryCombinationNeeded,
            boolean suffixTrace, boolean zigZag, boolean setAllTrueIfSat);
}
