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
package cpa.symbpredabs.summary;

import java.util.Map;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;

import cfa.objectmodel.CFANode;

import common.Pair;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * Formula manager that understands summary locations
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SummaryFormulaManager extends SymbolicFormulaManager {

    /**
     * computes the path formulas for each of the leaves of the inner subgraph
     * of the given summary location
     */
    public Map<CFANode, Pair<SymbolicFormula, SSAMap>> buildPathFormulas(
            SummaryCFANode summary) throws UnrecognizedCFAEdgeException;

}
