/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;

public interface PathFormulaManager {

  PathFormula makeEmptyPathFormula();

  PathFormula makeEmptyPathFormula(PathFormula oldFormula);

  /**
   * Creates a new path formula representing an OR of the two arguments. Differently
   * from {@link FormulaManager#makeOr(Formula, Formula)},
   * it also merges the SSA maps and creates the necessary adjustments to the
   * formulas if the two SSA maps contain different values for the same variables.
   *
   * @param pF1 a PathFormula
   * @param pF2 a PathFormula
   * @return (pF1 | pF2)
   */
  PathFormula makeOr(PathFormula pF1, PathFormula pF2);

  PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula);

  PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge) throws CPATransferException;

  PathFormula makeAnd(PathFormula pMergedPathFormula, CFAEdge pLocalEdge,
      int pThreadId) throws CPATransferException;


  PathFormula makeNewPathFormula(PathFormula pOldFormula, SSAMap pM);

  PathFormula shiftFormula(PathFormula formula, int offset);

  PathFormula makeAnd(PathFormula pPathFormula, PathFormula pShiftedEnvPathFormula);

  PathFormula makeAnd(PathFormula pPathFormula, Formula foruma, int tid);





}