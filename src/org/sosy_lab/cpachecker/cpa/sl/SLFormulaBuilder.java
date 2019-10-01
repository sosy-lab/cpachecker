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
package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.Formula;

public interface SLFormulaBuilder {

  /**
   * Evaluates a CEpression's numeric value.
   *
   * @param pExp - The expression to be evaluated.
   * @param usePredContext - true if the SSAIndices should be inferred by the predecessor context
   *        (e.g. RHS).
   * @return numeric value
   */
  public BigInteger getValueForCExpression(CExpression pExp, boolean usePredContext)
      throws Exception;

  /**
   * Returns a formula for the given variable name
   *
   * @param pVariable - the variable name.
   * @param usePredContext - true if the SSAIndices should be inferred by the predecessor context
   *        (e.g. RHS).
   * @return formula of variable
   */
  public Formula
      getFormulaForVariableName(String pVariable, boolean usePredContext);

  /**
   * Returns a formula for the given variable name
   *
   * @param pDecl - the variable declaration.
   * @return formula of variable
   */
  public Formula getFormulaForDeclaration(CSimpleDeclaration pDecl);

  /**
   *
   * @param pExp - the expression.
   * @param usePredContext - true if the SSAIndices should be inferred by the predecessor context
   *        (e.g. RHS).
   * @return Formula - the formula representing the given @Expression.
   */
  public Formula getFormulaForExpression(CExpression pExp, boolean usePredContext) throws Exception;

  PathFormula getPathFormula();

  PathFormula getPredPathFormula();

  void updateSSAMap(SSAMap pMap);
}
