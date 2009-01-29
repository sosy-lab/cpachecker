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
package cpa.itpabs;

import java.util.HashMap;
import java.util.Map;

import logging.LazyLogger;

import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabs.ConcreteTrace;
import cpa.symbpredabs.SymbolicFormula;


/**
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 * 
 * An class that stores information about a counterexample trace. For
 * real counterexamples, this stores the actual execution trace leading to
 * the error. For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpCounterexampleTraceInfo {
    private boolean spurious;
    private Map<AbstractElement, SymbolicFormula> refmap;
    private ConcreteTrace ctrace;

    public ItpCounterexampleTraceInfo(boolean spurious) {
        this.spurious = spurious;
        refmap = new HashMap<AbstractElement, SymbolicFormula>();
        ctrace = null;
    }
    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }

    public SymbolicFormula getFormulaForRefinement(AbstractElement e) {
        if (refmap.containsKey(e)) {
            return refmap.get(e);
        } else {
            return null;
        }
    }

    public void setFormulaForRefinement(AbstractElement e, SymbolicFormula f) {
        LazyLogger.log(LazyLogger.DEBUG_3, "SETTING REFINEMENT FOR ", e,
                ": ", f);
        refmap.put(e, f);
    }

    /**
     * for real counterexamples, returns the concrete execution trace leading
     * to the error
     * @return a ConcreteTrace from the entry point of the program to an error
     *         location
     */
    public ConcreteTrace getConcreteTrace() { return ctrace; }

    public void setConcreteTrace(ConcreteTrace ctrace) {
        this.ctrace = ctrace;
    }
}
