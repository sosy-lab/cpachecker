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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.FUNCTION_DECLARATION_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.FUNCTION_TYPE;

import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Management of formula encoding of function objects and their association with a function
 * declaration, which is required to resolve a dynamic function call (call of a function object).
 * The association is accomplished using an uninterpreted function formula called <code>
 * declarationOf</code>.
 *
 * @see org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression
 * @see org.sosy_lab.cpachecker.cfa.parser.eclipse.js.UnknownFunctionCallerDeclarationBuilder
 */
class JSFunctionDeclarationFormulaManager {
  private final FunctionFormulaManagerView ffmgr;
  private final FunctionDeclaration<IntegerFormula> declarationOfDeclaration;

  JSFunctionDeclarationFormulaManager(final FunctionFormulaManagerView pFfmgr) {
    ffmgr = pFfmgr;
    declarationOfDeclaration =
        ffmgr.declareUF("declarationOf", FUNCTION_DECLARATION_TYPE, FUNCTION_TYPE);
  }

  IntegerFormula declarationOf(final IntegerFormula pFunctionObject) {
    return ffmgr.callUF(declarationOfDeclaration, pFunctionObject);
  }
}
