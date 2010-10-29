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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import java.util.ArrayList;
import java.util.Collection;

import org.sosy_lab.cpachecker.exceptions.CPAException;

import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.trace.CounterexampleTraceInfo;


/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbPredAbsFormulaManager extends FormulaManager {

    /**
     * Abstract post operation.
     */
    public AbstractFormula buildAbstraction(
            AbstractFormula abs, PathFormula pathFormula,
            Collection<Predicate> predicates);

    /**
     * Counterexample analysis and predicate discovery.
     * @throws CPAException
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            ArrayList<AbstractionElement> abstractTrace) throws CPAException;

    /**
     * Checks if an abstraction formula and a pathFormula are unsatisfiable.
     * @param pAbstractionFormula the abstraction formula
     * @param pPathFormula the path formula
     * @return unsat(pAbstractionFormula & pPathFormula)
     */
    boolean unsat(AbstractFormula pAbstractionFormula, PathFormula pPathFormula);
    
    /**
     * Checks if (a1 & p1) => a2
     */
    boolean checkCoverage(AbstractFormula a1, PathFormula p1, AbstractFormula a2);
}
