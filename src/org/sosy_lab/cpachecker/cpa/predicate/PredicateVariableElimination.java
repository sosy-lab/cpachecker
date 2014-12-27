/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class PredicateVariableElimination {

  // TODO: We might want to move this code to the class FormulaManagerView

  public static List<Formula> getDeadVariables(FormulaManagerView pFmv, BooleanFormula pFormula, SSAMap pSsa) {
    Set<Triple<Formula, String, Integer>> formulaVariables = pFmv.extractFreeVariables(pFormula);
    List<Formula> result = Lists.newArrayList();

    for (Triple<Formula, String, Integer> var: formulaVariables) {

      Formula varFormula = var.getFirst();
      String varName = var.getSecond();
      Integer varSsaIndex = var.getThird();

      if (varSsaIndex == null) {
        if (pSsa.containsVariable(varName)) {
          result.add(varFormula);
        }

      } else {

        if (varSsaIndex != pSsa.getIndex(varName)) {
          result.add(varFormula);
        }
      }
    }

    return result;
  }

  public static BooleanFormula eliminateDeadVariables(
      final FormulaManagerView pFmv,
      final BooleanFormula pF,
      final SSAMap pSsa)
    throws SolverException, InterruptedException {

    Preconditions.checkNotNull(pFmv);
    Preconditions.checkNotNull(pF);
    Preconditions.checkNotNull(pSsa);

    List<Formula> irrelevantVariables = getDeadVariables(pFmv, pF, pSsa);

    BooleanFormula eliminationResult = pF;

    if (!irrelevantVariables.isEmpty()) {
      QuantifiedFormulaManagerView qfmgr = pFmv.getQuantifiedFormulaManager();
      BooleanFormula quantifiedFormula = qfmgr.exists(irrelevantVariables, pF);
      eliminationResult = qfmgr.eliminateQuantifiers(quantifiedFormula);
    }

    eliminationResult = pFmv.simplify(eliminationResult); // TODO: Benchmark the effect!
    return eliminationResult;
  }

}
