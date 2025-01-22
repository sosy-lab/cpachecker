// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import com.google.common.base.Preconditions;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public final class CFloatLiteralExpression extends AFloatLiteralExpression
    implements CLiteralExpression {

  @Serial private static final long serialVersionUID = 5021145411123854111L;

  public CFloatLiteralExpression(
      FileLocation pFileLocation, MachineModel pMachineModel, CType pType, FloatValue pValue) {
    super(pFileLocation, pType, pValue);
    // Make sure that the provided type matches the type of the float value
    Preconditions.checkArgument(
        FloatValue.Format.fromCType(pMachineModel, pType).equals(pValue.getFormat()));
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CFloatLiteralExpression && super.equals(obj);
  }

  @Override
  public String toASTString() {
    FloatValue value = getValue();
    if (value.isInfinite()) {
      // "Infinity" is not a valid float literal
      // We need to rewrite it as the expression 1.0/0.0
      return (value.isNegative() ? "-" : "") + "1.0/0.0";
    } else if (value.isNan()) {
      // Same for NaN: It needs to be replaced by the expression 0.0/0.0
      return (value.isNegative() ? "-" : "") + "0.0/0.0";
    } else {
      // We have a regular value: print the number and add a suffix if necessary
      String repr = value.toString();

      // Add a suffix for "float" and "long double"
      CSimpleType type = (CSimpleType) getExpressionType();
      String suffix =
          type.equals(CNumericTypes.FLOAT)
              ? "f"
              : type.equals(CNumericTypes.LONG_DOUBLE) ? "l" : "";

      return repr + suffix;
    }
  }
}
