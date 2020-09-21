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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;

public class FunctionIndexer {

  private final Solver solver;
  private final CtoFormulaTypeHandler typeHandler;
  private final PathFormulaManager pathFormulaManager;

  public FunctionIndexer(
      Solver pSolver,
      CtoFormulaTypeHandler pTypeHandler,
      PathFormulaManager pPathFormulaManager) {
    solver = pSolver;
    typeHandler = pTypeHandler;
    pathFormulaManager = pPathFormulaManager;
  }

  private boolean
      isBitvectorEqualityFormula(BooleanFormula pAtom, BooleanFormulaManagerView pFormulaManager) {
    Boolean result = pFormulaManager.visit(pAtom, new DefaultBooleanFormulaVisitor<Boolean>() {

      @Override
      public Boolean visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> funcDecl) {
        if (funcDecl.getKind() == FunctionDeclarationKind.EQ) {
          return true;
        }
        return false;
      }

      @Override
      protected Boolean visitDefault() {
        return false;
      }

    });

    return result;
  }

  public BooleanFormula renameExternFunctionCalls(
      PredicateAbstractState pPredicateState,
      Map<AFunctionDeclaration, List<Formula>> pNewNamesForFunctions,
      Set<AFunctionDeclaration> pUnimplementedPointerReturnTypeFunctions) {

    BooleanFormula targetStateFormula = pPredicateState.getPathFormula().getFormula();

    FormulaManagerView formulaManagerView = solver.getFormulaManager();

    ImmutableSet<BooleanFormula> atoms =
        formulaManagerView.extractAtoms(targetStateFormula, true);
    Set<Map<BooleanFormula, BooleanFormula>> substitutionMaps = new HashSet<>();

    for (AFunctionDeclaration externFunctionName : pUnimplementedPointerReturnTypeFunctions) {
      // go through the formula and index function calls
      Formula constantFunctionFormula =
          formulaManagerView.makeVariableWithoutSSAIndex(
              typeHandler.getPointerType(),
              externFunctionName.getName());
      String constantFunctionString = constantFunctionFormula.toString();
      Map<BooleanFormula, BooleanFormula> substitutionMap = new HashMap<>();
      StringReplacer stringReplacer =
          new StringReplacer(
              constantFunctionString,
              pathFormulaManager,
              pPredicateState);
      for (BooleanFormula atom : Lists.reverse(atoms.asList())) {

        Set<String> atomVariables = formulaManagerView.extractVariableNames(atom);

        boolean isBitVectorEqualityFormula =
            isBitvectorEqualityFormula(atom, formulaManagerView.getBooleanFormulaManager());

        if (atomVariables.contains(constantFunctionString) && isBitVectorEqualityFormula) {
          BooleanFormula newAtom =
              formulaManagerView
                  .renameFreeVariablesAndUFs(atom, name -> stringReplacer.renameField(name));
          substitutionMap.put(atom, newAtom);
        }
      }
      substitutionMaps.add(substitutionMap);
      pNewNamesForFunctions.put(externFunctionName, stringReplacer.getNewNames());
    }

    BooleanFormula newFinalFormula = targetStateFormula;
    for (Map<BooleanFormula, BooleanFormula> substitutionMap : substitutionMaps) {
      newFinalFormula = formulaManagerView.substitute(newFinalFormula, substitutionMap);
    }
    return newFinalFormula;
  }

}
