/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.transition;

import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;

final class Reindexer {

  private Reindexer() {}

  public static <F extends Formula> F invertIndices(
      F pOldFormula, SSAMap pFormulaSSAMap, FormulaManagerView pFormulaManager) {
    return invertIndices(pOldFormula, pFormulaSSAMap, 1, pFormulaManager);
  }

  public static <F extends Formula> F invertIndices(
      F pOldFormula, SSAMap pFormulaSSAMap, int pLowIndex, FormulaManagerView pFormulaManager) {
    Set<String> allVariables = pFormulaSSAMap.allVariables();
    Map<String, IntUnaryOperator> substitution =
        Maps.newHashMapWithExpectedSize(allVariables.size());
    Set<String> formulaVariables = pFormulaManager.extractVariableNames(pOldFormula);
    for (String variableName : allVariables) {
      int highIndex = pFormulaSSAMap.getIndex(variableName);
      String highVariableName =
          pFormulaManager
              .instantiate(Collections.singleton(variableName), pFormulaSSAMap)
              .iterator()
              .next();
      final int realHighIndex;
      if (formulaVariables.contains(highVariableName)) {
        realHighIndex = highIndex;
      } else {
        realHighIndex = highIndex - 1;
      }
      substitution.put(variableName, index -> realHighIndex - index + pLowIndex);
    }
    return reindex(
        pOldFormula,
        pFormulaSSAMap,
        (var, i) -> substitution.get(var).applyAsInt(i),
        pFormulaManager);
  }

  public static <F extends Formula> SSAMap adjustToFormula(
      F pFormula, SSAMap pFormulaSSAMap, FormulaManagerView pFormulaManager) {
    Set<String> formulaVariables = pFormulaManager.extractVariableNames(pFormula);
    SSAMapBuilder builder = SSAMap.emptySSAMap().builder();
    for (String variableName : pFormulaSSAMap.allVariables()) {
      CType type = pFormulaSSAMap.getType(variableName);
      int highIndex = pFormulaSSAMap.getIndex(variableName);
      String highVariableName =
          pFormulaManager
              .instantiate(Collections.singleton(variableName), pFormulaSSAMap)
              .iterator()
              .next();
      while (highIndex > 1 && !formulaVariables.contains(highVariableName)) {
        --highIndex;
        highVariableName =
            pFormulaManager
                .instantiate(
                    Collections.singleton(variableName),
                    SSAMap.emptySSAMap().builder().setIndex(variableName, type, highIndex).build())
                .iterator()
                .next();
      }
      final int realHighIndex = highIndex;
      builder.setIndex(variableName, type, realHighIndex);
    }
    return builder.build();
  }

  public static <F extends Formula> F reindex(
      F pOldFormula,
      SSAMap pFormulaSSAMap,
      BiFunction<String, Integer, Integer> pConvertIndex,
      FormulaManagerView pFormulaManager) {
    return reindex(pOldFormula, pFormulaSSAMap, 1, pConvertIndex, pFormulaManager);
  }

  public static <F extends Formula> F reindex(
      F pOldFormula,
      SSAMap pFormulaSSAMap,
      int pLowIndex,
      BiFunction<String, Integer, Integer> pConvertIndex,
      FormulaManagerView pFormulaManager) {
    Set<String> allVariables = pFormulaSSAMap.allVariables();
    Set<String> allIndexedVariables = pFormulaManager.extractVariableNames(pOldFormula);
    Map<String, String> substitution = Maps.newHashMapWithExpectedSize(allVariables.size());
    for (String variableName : allVariables) {
      CType type = pFormulaSSAMap.getType(variableName);
      int highIndex = pFormulaSSAMap.getIndex(variableName);
      for (int index = pLowIndex; index <= highIndex; ++index) {
        SSAMap oldSSAMap =
            SSAMap.emptySSAMap().builder().setIndex(variableName, type, index).build();
        String oldVariableName =
            pFormulaManager
                .instantiate(Collections.singleton(variableName), oldSSAMap)
                .iterator()
                .next();
        if (allIndexedVariables.contains(oldVariableName)) {
          int newIndex = pConvertIndex.apply(variableName, index);
          SSAMap newSSAMap =
              SSAMap.emptySSAMap().builder().setIndex(variableName, type, newIndex).build();
          String newVariableName =
              pFormulaManager
                  .instantiate(Collections.singleton(variableName), newSSAMap)
                  .iterator()
                  .next();
          substitution.put(oldVariableName, newVariableName);
        }
      }
    }
    return pFormulaManager.renameFreeVariablesAndUFs(
        pOldFormula,
        (oldName) -> {
          String substitute = substitution.get(oldName);
          return substitute != null ? substitute : oldName;
        });
  }

}
