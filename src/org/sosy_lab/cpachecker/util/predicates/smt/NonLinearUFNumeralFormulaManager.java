/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;
import org.sosy_lab.java_smt.api.UFManager;

/**
 * As not all solvers support non-linear arithmetics,
 * we use uninterpreted functions whenever a direct operation fails.
 *
 * <p>
 * This behaviour depends on the solver and differs for different solvers,
 * if they support more or less operations.
 * </p>
 */
class NonLinearUFNumeralFormulaManager
    <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
    extends NumeralFormulaManagerView<ParamFormulaType, ResultFormulaType>  {

  private static final String UF_MULTIPLY_NAME = "_*_";
  private static final String UF_DIVIDE_NAME = "_/_";
  private static final String UF_MODULO_NAME = "_%_";

  private final FunctionDeclaration<ResultFormulaType> multUfDecl;
  private final FunctionDeclaration<ResultFormulaType> divUfDecl;
  private final FunctionDeclaration<ResultFormulaType> modUfDecl;

  private final UFManager functionManager;

  NonLinearUFNumeralFormulaManager(FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<ParamFormulaType, ResultFormulaType> numeralFormulaManager,
      UFManager pFunctionManager) {
    super(pWrappingHandler, numeralFormulaManager);
    functionManager = checkNotNull(pFunctionManager);
    FormulaType<ResultFormulaType> formulaType = manager.getFormulaType();

    multUfDecl = createBinaryFunction(UF_MULTIPLY_NAME, formulaType);
    divUfDecl = createBinaryFunction(UF_DIVIDE_NAME, formulaType);
    modUfDecl = createBinaryFunction(UF_MODULO_NAME, formulaType);
  }

  private FunctionDeclaration<ResultFormulaType> createBinaryFunction(
      String name, FormulaType<ResultFormulaType> formulaType) {
    return functionManager.declareUF(
        formulaType + "_" + name, formulaType, formulaType, formulaType);
  }

  private ResultFormulaType makeUf(
      FunctionDeclaration<ResultFormulaType> decl, ParamFormulaType t1, ParamFormulaType t2) {
    return functionManager.callUF(decl, ImmutableList.of(t1, t2));
  }

  @Override
  public ResultFormulaType divide(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    ResultFormulaType result;
    try {
      result = manager.divide(pNumber1, pNumber2);
    } catch (UnsupportedOperationException e) {
      result = makeUf(divUfDecl, pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public ResultFormulaType modulo(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    ResultFormulaType result;
    try {
      result = manager.modulo(pNumber1, pNumber2);
    } catch (UnsupportedOperationException e) {
      result = makeUf(modUfDecl, pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    ResultFormulaType result;
    try {
      result = manager.multiply(pNumber1, pNumber2);
    } catch (UnsupportedOperationException e) {
      result = makeUf(multUfDecl, pNumber1, pNumber2);
    }
    return result;
  }
}
