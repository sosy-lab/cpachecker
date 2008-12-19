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

import java.util.Collection;
import java.util.Deque;

import cfa.objectmodel.CFAEdge;

import cpa.itpabs.ItpAbstractElement;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SymbolicFormulaManager;

/**
 * An abstract formula manager for interpolation-based lazy abstraction.
 *
 * TODO - probably these two methods here should be moved to
 * AbstractFormulaManager, since pretty much every analysis that uses
 * AbstractFormulaManager re-defines them
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface ItpAbstractFormulaManager extends AbstractFormulaManager {

    public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
            ItpAbstractElement e, ItpAbstractElement succ,
            CFAEdge edge, Collection<Predicate> predicates);

    public CounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<ItpAbstractElement> abstractTrace);

}
