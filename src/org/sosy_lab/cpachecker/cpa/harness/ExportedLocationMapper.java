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

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class ExportedLocationMapper {

  private final FormulaManagerView formulaManagerView;

  public ExportedLocationMapper(FormulaManagerView pFormulaManagerView) {
    formulaManagerView = pFormulaManagerView;
  }

  public BooleanFormula getExportedLocationsToIndexFormula(
      List<Formula> pExternallyKnownPointers,
      PathFormulaManager pPathFormulaManager,
      FunctionDeclaration<?> pIndexFunctionDeclaration)
  {
    BooleanFormulaManagerView booleanFormulaManagerView =
        formulaManagerView.getBooleanFormulaManager();
    FunctionFormulaManagerView functionFormulaManagerView =
        formulaManagerView.getFunctionFormulaManager();
    IntegerFormulaManagerView integerFormulaManagerView =
        formulaManagerView.getIntegerFormulaManager();

    PathFormula emptyFormula = pPathFormulaManager.makeEmptyPathFormula();

    BooleanFormula resultConjunction = booleanFormulaManagerView.and(emptyFormula.getFormula());
    int length = pExternallyKnownPointers.size();
    for (int i = 0; i < length; i++) {

      Formula exportedExpression = pExternallyKnownPointers.get(i);

      IntegerFormula value = integerFormulaManagerView.makeNumber(i);

      IntegerFormula newBooleanFormula =
          (IntegerFormula) functionFormulaManagerView
              .callUF(pIndexFunctionDeclaration, exportedExpression);

      BooleanFormula valueFormula = integerFormulaManagerView.equal(newBooleanFormula, value);
      resultConjunction = booleanFormulaManagerView.and(resultConjunction, valueFormula);

    }

    return resultConjunction;
  }

}
