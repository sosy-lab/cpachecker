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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionToCExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class ValueToCExpressionTransformer implements ValueVisitor<CExpression> {

  private final SymbolicExpressionToCExpressionTransformer symbolicTransformer =
      new SymbolicExpressionToCExpressionTransformer();

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
    if (type instanceof CSimpleType) {
      switch (((CSimpleType) type).getType()) {
        case FLOAT:
        case DOUBLE:
          return visitFloatingValue(pValue, (CSimpleType) type);
        default:
          // DO NOTHING
          break;
      }
    }

    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, type, BigInteger.valueOf(pValue.longValue()));
  }

  private CExpression visitFloatingValue(NumericValue pValue, CSimpleType pType) {
    boolean isInfinite;
    boolean isNegative;
    boolean isNan;
    switch (pType.getType()) {
      case FLOAT:
        {
          float val = pValue.floatValue();
          isInfinite = Float.isInfinite(val);
          isNegative = val < 0;
          isNan = Float.isNaN(val);
          break;
        }
      case DOUBLE:
        {
          double val = pValue.doubleValue();
          isInfinite = Double.isInfinite(val);
          isNegative = val < 0;
          isNan = Double.isNaN(val);
          break;
        }
      default:
        throw new AssertionError("Unhandled type: " + pType);
    }

    assert !(isInfinite && isNan);

    if (isInfinite) {
      if (isNegative) {
        return CFloatLiteralExpression.forNegativeInfinity(FileLocation.DUMMY, pType);
      } else {
        return CFloatLiteralExpression.forPositiveInfinity(FileLocation.DUMMY, pType);
      }
    } else if (isNan) {
      return createNanExpression(pType);
    } else {
      return new CFloatLiteralExpression(FileLocation.DUMMY, pType, pValue.bigDecimalValue());
    }
  }

  private CExpression createNanExpression(CSimpleType pType) {
    // Represent NaN by '0/0', until CFloats are used by CFloatLiteralExpression
    CExpression zero = new CFloatLiteralExpression(FileLocation.DUMMY, pType, BigDecimal.ZERO);
    return new CBinaryExpression(
        FileLocation.DUMMY, pType, pType, zero, zero, CBinaryExpression.BinaryOperator.DIVIDE);
  }

  @Override
  public CExpression visit(NullValue pValue) {
    throw new UnsupportedOperationException(NullValue.class.getSimpleName() + " is a Java value");
  }
}
