/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.harness;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class FunctionToIndicesResolver {

  private final ProverEnvironment proverEnvironment;
  private final FunctionFormulaManagerView functionFormulaManagerView;

  public FunctionToIndicesResolver(
      ProverEnvironment pProverEnvironment,
      FunctionFormulaManagerView pFunctionFormulaManagerView) {
    proverEnvironment = pProverEnvironment;
    functionFormulaManagerView = pFunctionFormulaManagerView;
  }

  public Map<AFunctionDeclaration, List<Integer>> resolveFunctions(
      Set<AFunctionDeclaration> pUnimplementedPointerReturnTypeFunctions,
      Map<AFunctionDeclaration, List<Formula>> pNewNamesForFunctions,
      FunctionDeclaration<?> pIndexFunctionDeclaration)
      throws SolverException, InterruptedException {
    Map<AFunctionDeclaration, List<Integer>> indicesOfFunctions = new HashMap<>();
    if (!proverEnvironment.isUnsat()) {

      for (AFunctionDeclaration functionDeclaration : pUnimplementedPointerReturnTypeFunctions) {
        List<Formula> values = pNewNamesForFunctions.get(functionDeclaration);
        int size = values.size();
        List<Integer> indices = new LinkedList<>();
        for (int i = 0; i < size; i++) {
          Formula value = values.get(i);
          IntegerFormula newIntegerFormula =
              (IntegerFormula) functionFormulaManagerView.callUF(pIndexFunctionDeclaration, value);
          @Nullable
          BigInteger index = proverEnvironment.getModel().evaluate(newIntegerFormula);

          int intIndex = index.intValue();
          indices.add(intIndex);
        }
        indicesOfFunctions.put(functionDeclaration, indices);
      }
    }
    return indicesOfFunctions;
  }

}
