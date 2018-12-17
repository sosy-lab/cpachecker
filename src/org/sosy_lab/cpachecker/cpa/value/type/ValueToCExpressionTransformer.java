/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.type;


import java.math.BigDecimal;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class ValueToCExpressionTransformer implements ValueVisitor<CExpression> {

  private final SymbolicExpressionTransformer symbolicTransformer =
      new SymbolicExpressionTransformer();

  private CType type;

  public ValueToCExpressionTransformer(CType pTypeOfValue) {
    type = pTypeOfValue;
  }

  @Override
  public CExpression visit(EnumConstantValue pValue) {
    throw new UnsupportedOperationException(
        EnumConstantValue.class.getSimpleName() + " is a Java" + " value");
  }

  @Override
  public CExpression visit(SymbolicValue pValue) {
    return pValue.accept(symbolicTransformer);
  }

  @Override
  public CExpression visit(UnknownValue pValue) {
    throw new UnsupportedOperationException("Unknown values can't be transformed to CExpressions");
  }

  @Override
  public CExpression visit(ArrayValue pValue) {
    throw new UnsupportedOperationException(
        ArrayValue.class.getSimpleName() + " is a Java" + " value");
  }

  @Override
  public CExpression visit(BooleanValue pValue) {
    BigInteger asInt;
    if (pValue.isTrue()) {
      asInt = BigInteger.ONE;
    } else {
      asInt = BigInteger.ZERO;
    }
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, asInt);
  }

  @Override
  public CExpression visit(FunctionValue pValue) {
    throw new UnsupportedOperationException(
        "Function values can't be transformed back " + "to CExpressions correctly, at the moment");
  }

  @Override
  public CExpression visit(NumericValue pValue) {
    if (type instanceof CSimpleType
        && (((CSimpleType) type).getType().equals(CBasicType.FLOAT)
            || ((CSimpleType) type).getType().equals(CBasicType.DOUBLE))) {
      return new CFloatLiteralExpression(
          FileLocation.DUMMY, type, BigDecimal.valueOf(pValue.doubleValue()));

    } else {
      return new CIntegerLiteralExpression(
          FileLocation.DUMMY, type, BigInteger.valueOf(pValue.longValue()));
    }
  }

  @Override
  public CExpression visit(NullValue pValue) {
    throw new UnsupportedOperationException(NullValue.class.getSimpleName() + " is a Java value");
  }
}
