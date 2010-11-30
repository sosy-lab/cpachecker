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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import org.sosy_lab.cpachecker.util.symbpredabstraction.AbstractionFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Formula;


/**
 * An AbstractFormulaManager that knows about Summary locations.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SymbPredAbsFormulaManager extends PathFormulaManager {

    /**
     * Abstract post operation.
     */
    public AbstractionFormula buildAbstraction(
            AbstractionFormula abs, PathFormula pathFormula,
            Collection<AbstractionPredicate> predicates);

    /**
     * Counterexample analysis and predicate discovery.
     * @throws CPAException
     */
    public CounterexampleTraceInfo buildCounterexampleTrace(
            ArrayList<SymbPredAbsAbstractElement> abstractTrace) throws CPAException;

    /**
     * Checks if an abstraction formula and a pathFormula are unsatisfiable.
     * @param pAbstractionFormulaFormula the abstraction formula
     * @param pPathFormula the path formula
     * @return unsat(pAbstractionFormula & pPathFormula)
     */
    boolean unsat(AbstractionFormula pAbstractionFormulaFormula, PathFormula pPathFormula);
    
    /**
     * Checks if (a1 & p1) => a2
     */
    boolean checkCoverage(AbstractionFormula a1, PathFormula p1, AbstractionFormula a2);

    /**
     * Create predicates for all atoms in a formula.
     */
    List<AbstractionPredicate> getAtomsAsPredicates(Formula pF);

    CounterexampleTraceInfo checkPath(List<CFAEdge> pPath) throws CPATransferException;

    void dumpCounterexampleToFile(CounterexampleTraceInfo pCex, File pFile);

    
    // methods from AbstractionManager
    
    AbstractionPredicate makeFalsePredicate();

    AbstractionFormula makeTrueAbstractionFormula(Formula previousBlockFormula);
}
