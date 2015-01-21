/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpressionFactory;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;

/**
 * Class for transforming {@link AExpression}s to {@link SymbolicExpression}s.
 *
 * <p>For each transformation, a new object has to be created. Otherwise, the resulting expressions might not reflect the
 * programs possible concrete states.</p>
 *
 */
public class ExpressionTransformer {

  private final String functionName;

  private boolean missingInformation = false;
  private Optional<ValueAnalysisState> valueState;

  public ExpressionTransformer(String pFunctionName, Optional<ValueAnalysisState> pValueState) {
    functionName = pFunctionName;
    valueState = pValueState;
  }

  protected Value createNumericValue(long pValue) {
    return new NumericValue(pValue);
  }

  protected Value createNumericValue(BigDecimal pValue) {
    return new NumericValue(pValue);
  }

  protected Value createNumericValue(BigInteger pValue) {
    return new NumericValue(pValue);
  }

  /**
   * Returns whether information was missing while transforming the last expression.
   *
   * <p>This method always resets after one call. So when calling this method after the creation of a formula,
   * it will only return <code>true</code> at the first call, if at all.</p>
   *
   * @return <code>true</code> if information was missing, <code>false</code> otherwise
   */
  public boolean hasMissingInformation() {
    return missingInformation;
  }

  public SymbolicExpression visit(AIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    if (!valueState.isPresent()) {
      missingInformation = true;
      return null;
    }

    final ValueAnalysisState state = valueState.get();
    final MemoryLocation memLoc = getMemoryLocation(pIastIdExpression.getDeclaration());

    final Type idType = pIastIdExpression.getExpressionType();

    if (state.contains(memLoc)) {
      final Value idValue = state.getValueFor(memLoc);

      return SymbolicExpressionFactory.getInstance().asConstant(idValue, idType);

    } else {
      return null;
    }
  }

  private MemoryLocation getMemoryLocation(ASimpleDeclaration pDeclaration) {
    if (pDeclaration instanceof ADeclaration && ((ADeclaration) pDeclaration).isGlobal()) {
      return MemoryLocation.valueOf(pDeclaration.getName());

    } else {
      return MemoryLocation.valueOf(functionName, pDeclaration.getName(), 0);
    }
  }

  public SymbolicExpression visit(AIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    final BigInteger value = pIastIntegerLiteralExpression.getValue();
    final Type intType = pIastIntegerLiteralExpression.getExpressionType();

    return SymbolicExpressionFactory.getInstance().asConstant(createNumericValue(value), intType);
  }

  public SymbolicExpression visit(ACharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    final long castValue = (long) pIastCharLiteralExpression.getCharacter();
    final Type charType = pIastCharLiteralExpression.getExpressionType();

    return SymbolicExpressionFactory.getInstance().asConstant(createNumericValue(castValue), charType);
  }

  public SymbolicExpression visit(AFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    final BigDecimal value = pIastFloatLiteralExpression.getValue();
    final Type floatType = pIastFloatLiteralExpression.getExpressionType();

    return SymbolicExpressionFactory.getInstance().asConstant(createNumericValue(value), floatType);
  }

}
