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

import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeFormulaTemplate;
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

  PathFormula makeAnd(PathFormula pf1, PathFormula pf2);

  PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula);

  PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge) throws CPATransferException;

  Triple<Formula, SSAMap, Integer> makeAnd2(PathFormula localF, CFAEdge edge ) throws CPATransferException ;


  Pair<PathFormula, RelyGuaranteeFormulaTemplate> makeTemplateAnd(PathFormula oldLocalF, CFAEdge edge, RelyGuaranteeFormulaTemplate oldTemplate) throws CPATransferException;


  PathFormula makeNewPathFormula(PathFormula pOldFormula, SSAMap pM);


  PathFormula matchPaths(PathFormula localPF, PathFormula envPF, Set<String> globalVariablesSet);

  PathFormula buildEqualitiesOverVariables(PathFormula pf1, PathFormula pf2, Set<String> variableSet);


  // returns an empty path formula with a clean SSAMap from variables that do not belong to this thread
  PathFormula makeEmptyPathFormula(PathFormula pPathFormula,  int pThreadId);

  PathFormula primePathFormula(PathFormula envPF, int offset);


  // for testing...
  Formula buildLvalueTerm(IASTExpression exp, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException;

  CFAEdge inject(CFAEdge pLocalEdge, Set<String> pGlobalVariablesSet, int pOffset, SSAMap pSsa) throws CPATransferException;

  PathFormula normalize(PathFormula pNewPF);


}