// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibRealConstantTerm implements SvLibConstantTerm {

  @Serial private static final long serialVersionUID = 9089449138512195005L;
  private final Rational value;
  private final FileLocation fileLocation;

  public SvLibRealConstantTerm(Rational pValue, FileLocation pFileLocation) {
    value = pValue;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public SvLibType getExpressionType() {
    return SvLibSmtLibPredefinedType.REAL;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    // SMT-LIB has no negative numerals, -1 would be a symbol. A decimal is a real in every logic,
    // while a numeral is one only in a logic without integers.
    BigInteger numerator = value.getNum().abs();
    BigInteger denominator = value.getDen();
    String absoluteValue;
    try {
      BigDecimal decimal =
          new BigDecimal(numerator).divide(new BigDecimal(denominator), MathContext.UNLIMITED);
      absoluteValue =
          decimal.scale() > 0 ? decimal.toPlainString() : decimal.toPlainString() + ".0";
    } catch (ArithmeticException e) {
      // The value has no finite decimal expansion
      absoluteValue = "(/ " + numerator + " " + denominator + ")";
    }
    return value.getNum().signum() < 0 ? "(- " + absoluteValue + ")" : absoluteValue;
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public Rational getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibRealConstantTerm other && value.equals(other.value);
  }
}
