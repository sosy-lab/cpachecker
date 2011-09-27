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
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;

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

  /**
   * Creates a new path formula representing an OR of the two arguments.
   * Variables primed i times are adjusted to the same index.
   * @param pf1
   * @param pf2
   * @param i
   * @return
   */
  PathFormula makeRelyGuaranteeOr(PathFormula pf1, PathFormula pf2, int i);

  PathFormula makeAnd(PathFormula pf1, PathFormula pf2);

  PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula);

  PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge) throws CPATransferException;

  PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge, Integer tid) throws CPATransferException;


  PathFormula makeNewPathFormula(PathFormula pOldFormula, SSAMap pM);

  PathFormula matchPaths(PathFormula localPF, PathFormula envPF, Set<String> globalVariablesSet, int offset);

  PathFormula buildEqualitiesOverVariables(PathFormula pf1, PathFormula pf2, Set<String> variableSet);


  // returns an empty path formula with a clean SSAMap from variables that do not belong to this thread
  PathFormula makeEmptyPathFormula(PathFormula pPathFormula,  int pThreadId);

  PathFormula primePathFormula(PathFormula envPF, int offset);


  // for testing...
  Formula buildLvalueTerm(IASTExpression exp, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException;

  void inject(CFAEdge localEdge, Set<String> globalVariablesSet, int offset, Integer tid, SSAMap pSsa) throws CPATransferException;

  PathFormula normalize(PathFormula pNewPF);

  PathFormula makeFalsePathFormula();

  PathFormula adjustPrimedNo(PathFormula pathFormula, Map<Integer, Integer> primedMap);

  PathFormula primePathFormula(PathFormula pEnvPF, int pOffset, SSAMap pSsa);

  /**
   * Remove atoms that consists only of primed variables.
   * @param pathFormula
   * @param primedNo
   * @return
   */
  PathFormula removePrimed(PathFormula pathFormula, Set<Integer> primedNo);

  /**
   * Makes conjunction of equalities v^i@x <-> v^j@x, where x>0 is the last index of
   * variable v^i in the path formula and v^j is v primed j times.
   * @param pf
   * @param i
   * @param pUniquePrime
   * @return
   */
  PathFormula makePrimedEqualities(PathFormula pf, int i, int j);

  /**
   * Builds unsatisfiable constraints for variables indexes that are between ssa and ssatop maps.
   * Only variables primed tid times are considered.
   * @param ssatop
   * @param ssa
   * @param tid
   * @return
   */
  PathFormula makeUnsatisifiableConstraintsForRedundantIndexes(SSAMap ssatop, SSAMap ssa, int tid);

  /**
   * Makes conjunction of equalities v^i@x <-> v^j@y, where x>0 is the last index of
   * variable v^i in pf1 and y>0 is the last index of v^j in pf2.
   * @param pf
   * @param i
   * @param pUniquePrime
   * @return
   */
  PathFormula makePrimedEqualities(PathFormula pf1, int i, PathFormula pf2, int j);


}