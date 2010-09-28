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
package org.sosy_lab.cpachecker.cpa.predicateabstraction;

import java.util.Collection;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.symbpredabstraction.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Formula Manager for Explicit-state predicate abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface PredicateAbstractionFormulaManager extends FormulaManager {

    /**
     * Computes the abstract post from "e" to "succ" on the given edge
     * wrt. the given set of predicates.
     */
    public AbstractFormula buildAbstraction(
            PredicateAbstractionAbstractElement e, PredicateAbstractionAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates);

    /**
     * Counterexample analysis. If the trace is spurious, the returned object
     * will carry information about the predicates needed for
     * refinement. Otherwise, it will contain information about the concrete
     * execution path leading to the error.
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            Pair<ARTElement, CFAEdge>[] pathArray);


    public class ConcretePath {
        public Vector<SymbolicFormula> path;
        public SSAMap ssa;

        public ConcretePath(Vector<SymbolicFormula> p, SSAMap s) {
            path = p;
            ssa = s;
        }
    }

    public ConcretePath buildConcretePath(SymbolicFormulaManager mgr,
        Pair<ARTElement, CFAEdge>[] pathArray)
                throws CPATransferException;

    public Vector<SymbolicFormula> getUsefulBlocks(SymbolicFormulaManager mgr,
            Vector<SymbolicFormula> trace,
            boolean suffixTrace, boolean zigZag, boolean setAllTrueIfSat);
}
