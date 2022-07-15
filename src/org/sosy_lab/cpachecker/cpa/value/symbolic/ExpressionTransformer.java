// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionToCExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Class for transforming {@link AExpression}s to {@link SymbolicExpression}s.
 *
 * <p>For each transformation, a new object has to be created. Otherwise, the resulting expressions
 * might not reflect the correct concrete states of the program.
 *
 * @see SymbolicExpressionToCExpressionTransformer
 */
public class ExpressionTransformer {

  protected final String functionName;

  protected ValueAnalysisState valueState;

  public ExpressionTransformer(String pFunctionName, ValueAnalysisState pValueState) {
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

  public SymbolicExpression visit(AIdExpression pIastIdExpression) {
    final MemoryLocation memLoc = getMemoryLocation(pIastIdExpression.getDeclaration());

    final Type idType = pIastIdExpression.getExpressionType();

    if (valueState.contains(memLoc)) {
      final Value idValue = valueState.getValueFor(memLoc);

      return SymbolicValueFactory.getInstance().asConstant(idValue, idType).copyForLocation(memLoc);

    } else {
      return null;
    }
  }

  private MemoryLocation getMemoryLocation(ASimpleDeclaration pDeclaration) {
    if (pDeclaration instanceof ADeclaration && ((ADeclaration) pDeclaration).isGlobal()) {
      return MemoryLocation.parseExtendedQualifiedName(pDeclaration.getName());

    } else {
      return MemoryLocation.forLocalVariable(functionName, pDeclaration.getName());
    }
  }

  public SymbolicExpression visit(AIntegerLiteralExpression pIastIntegerLiteralExpression) {
    final BigInteger value = pIastIntegerLiteralExpression.getValue();
    final Type intType = pIastIntegerLiteralExpression.getExpressionType();

    return SymbolicValueFactory.getInstance().asConstant(createNumericValue(value), intType);
  }

  public SymbolicExpression visit(ACharLiteralExpression pIastCharLiteralExpression) {
    final long castValue = pIastCharLiteralExpression.getCharacter();
    final Type charType = pIastCharLiteralExpression.getExpressionType();

    return SymbolicValueFactory.getInstance().asConstant(createNumericValue(castValue), charType);
  }

  public SymbolicExpression visit(AFloatLiteralExpression pIastFloatLiteralExpression) {
    final BigDecimal value = pIastFloatLiteralExpression.getValue();
    final Type floatType = pIastFloatLiteralExpression.getExpressionType();

    return SymbolicValueFactory.getInstance().asConstant(createNumericValue(value), floatType);
  }
}
