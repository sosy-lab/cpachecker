// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serial;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * A floating point type of the SMT-LIB theory of floating point numbers, given by the number of
 * bits of its exponent and of its significand.
 *
 * <p>The number of bits of the significand includes the hidden bit, as in SMT-LIB, so a {@code
 * double} is {@code (_ FloatingPoint 11 53)}.
 */
public final class SvLibSmtLibFloatingPointType implements SvLibSmtLibType {

  @Serial private static final long serialVersionUID = 8033159104716105191L;
  private final int exponentSize;
  private final int significandSize;

  public SvLibSmtLibFloatingPointType(int pExponentSize, int pSignificandSize) {
    checkArgument(pExponentSize > 1, "The exponent must have more than one bit");
    checkArgument(pSignificandSize > 1, "The significand must have more than one bit");
    exponentSize = pExponentSize;
    significandSize = pSignificandSize;
  }

  public int getExponentSize() {
    return exponentSize;
  }

  public int getSignificandSize() {
    return significandSize;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    return FormulaType.getFloatingPointTypeFromSizesWithHiddenBit(exponentSize, significandSize);
  }

  @Override
  public String toASTString() {
    return "(_ FloatingPoint " + exponentSize + " " + significandSize + ")";
  }

  @Override
  public String toASTString(String declarator) {
    return declarator + " : " + toASTString();
  }

  @Override
  public int hashCode() {
    return 31 * exponentSize + significandSize;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSmtLibFloatingPointType other
        && exponentSize == other.exponentSize
        && significandSize == other.significandSize;
  }
}
