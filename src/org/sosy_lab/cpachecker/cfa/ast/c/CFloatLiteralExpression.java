// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

public final class CFloatLiteralExpression extends AFloatLiteralExpression
    implements CLiteralExpression {

  private static final long serialVersionUID = 5021145411123854111L;

  public CFloatLiteralExpression(
      FileLocation pFileLocation, MachineModel pMachineModel, CType pType, FloatValue pValue) {
    super(pFileLocation, pType, matchType(pMachineModel, pType, pValue));
  }

  /**
   * Convert the value to the type of the floating point literal
   *
   * <p>This is needed to handle implicit casts in the code. Only upcasting is allowed, except for
   * NaN and Infinity, where downcasting won't cause any loss of precision. Throws an exception if
   * the type of the literal can't be matched.
   */
  private static FloatValue matchType(MachineModel pMachineModel, CType pType, FloatValue pValue) {
    Format format = pValue.getFormat();
    Format target = Format.fromCType(pMachineModel, pType);

    if (pValue.isNan() || pValue.isInfinite() || format.join(target).equals(target)) {
      return pValue.withPrecision(target);
    } else {
      throw new IllegalArgumentException();
    }
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
    // Print the value
    String repr = getValue().toString();

    // Add a suffix if the literal has type "float" or "long double"
    CSimpleType type = (CSimpleType) getExpressionType();
    String suffix =
        type.equals(CNumericTypes.FLOAT) ? "f" : type.equals(CNumericTypes.LONG_DOUBLE) ? "l" : "";

    return repr + suffix;
  }
}
