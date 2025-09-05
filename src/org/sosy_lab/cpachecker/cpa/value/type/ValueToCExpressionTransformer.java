// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionToCExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public class ValueToCExpressionTransformer implements ValueVisitor<CExpression> {

  private final SymbolicExpressionToCExpressionTransformer symbolicTransformer;

  private final MachineModel machineModel;
  private final CType type;

  public ValueToCExpressionTransformer(MachineModel pMachineModel, CType pTypeOfValue) {
    symbolicTransformer = new SymbolicExpressionToCExpressionTransformer(pMachineModel);
    machineModel = pMachineModel;
    type = pTypeOfValue;
  }

  @Override
  public CExpression visit(EnumConstantValue pValue) {
    throw new UnsupportedOperationException(
        EnumConstantValue.class.getSimpleName() + " is a Java value");
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
    throw new UnsupportedOperationException(ArrayValue.class.getSimpleName() + " is a Java value");
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
        "Function values can't be transformed back to CExpressions correctly, at the moment");
  }

  @Override
  public CExpression visit(NumericValue pValue) {
    if (type instanceof CSimpleType cSimpleType) {
      switch (cSimpleType.getType()) {
        case FLOAT, DOUBLE -> {
          return visitFloatingValue(pValue, cSimpleType);
        }
        default -> {
          // DO NOTHING
        }
      }
    }

    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, pValue.bigIntegerValue());
  }

  private CExpression visitFloatingValue(NumericValue pValue, CSimpleType pType) {
    FloatValue.Format precision = FloatValue.Format.fromCType(machineModel, pType);
    return new CFloatLiteralExpression(
        FileLocation.DUMMY, machineModel, pType, pValue.floatingPointValue(precision));
  }

  @Override
  public CExpression visit(NullValue pValue) {
    throw new UnsupportedOperationException(NullValue.class.getSimpleName() + " is a Java value");
  }
}
