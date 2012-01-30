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

import java.util.Map;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;

public interface PathFormulaManager {

  PathFormula makeEmptyPathFormula();

  PathFormula makeFalsePathFormula();

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



  /** Note this function may cause many errors, since it implicitly instantiates the formula*/
  @Deprecated
  PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula);

  PathFormula makeAnd(PathFormula pPathFormula, PathFormula pOtherFormula);

  PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge) throws CPATransferException;

  PathFormula makeNewPathFormula(PathFormula pOldFormula, SSAMap pM);



  /**
   * Returns the effect of applying edge on pf, which is the difference between SP_edge(pf) and pf.
   * @param pf
   * @param edge
   * @return effect path formula
   * @throws CPATransferException
   */
  PathFormula operationPathFormula(PathFormula pf, CFAEdge edge) throws CPATransferException;

  /**
   * Makes conjunction of equalities v^i@x <-> v^j@y, where x>0 is the last index of
   * variable v^i in ssa1 and y>0 is the last index of v^j in ssa2.
   * Note that v^-1 is treated as v.
   * @param ssa1
   * @param i
   * @param ssa2
   * @param j
   * @return equalities
   */
  PathFormula makePrimedEqualities(SSAMap ssa1, int i, SSAMap ssa2, int j);

  /**
   * Changes prime numbers of variables as specified by the map. Value -1 means no prime number.
   * @param pathFormula
   * @param map
   * @return change path formula
   */
  PathFormula changePrimedNo(PathFormula pathFormula, Map<Integer, Integer> map);

  /**
   * Instantiates plain variables of f to the low SSA map and hashed variables to the high map.
   * @param pf
   * @param low
   * @param high
   * @return instantiated formula
   */
  PathFormula instantiateNextValue(Formula f, SSAMap low, SSAMap high);


}