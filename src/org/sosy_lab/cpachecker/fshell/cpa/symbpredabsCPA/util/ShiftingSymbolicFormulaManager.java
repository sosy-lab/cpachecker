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
package org.sosy_lab.cpachecker.fshell.cpa.symbpredabsCPA.util;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;


/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * A SymbolicFormulaManager is an object that can create/manipulate
 * SymbolicFormulas
 */
public interface ShiftingSymbolicFormulaManager extends SymbolicFormulaManager {

    /**
     * "shifts" forward all the variables in the formula f, of the amount
     * given by the input ssa. That is, variables x with index 1 in f will be
     * replaced by variables with index ssa.getIndex(x), vars with index 2 by
     * vars with index ssa.getIndex(x)+1, and so on.
     * Returns the new formula and the ssa map with the final index for each
     * variable
     * @param f the SymbolicFormula to shift
     * @param ssa the SSAMap to use for shifting
     * @return the shifted formula and the new SSA map
     */
    public Pair<SymbolicFormula, SSAMap> shift(SymbolicFormula f, SSAMap ssa);

    /**
     * The path formulas have an uninterpreted function :=
     * where an assignment should be. This method replaces all those appearances
     * by equalities (which is a valid representation of an assignment for a SSA
     * formula).
     */
    public SymbolicFormula replaceAssignments(SymbolicFormula f);

}