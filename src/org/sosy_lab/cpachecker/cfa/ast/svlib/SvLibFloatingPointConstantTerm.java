// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.java_smt.api.FloatingPointNumber;

/**
 * A literal of the SMT-LIB theory of floating point numbers, which is written as its three parts:
 * {@code (fp #b0 #b10000000000 #b1000...)}.
 */
public final class SvLibFloatingPointConstantTerm implements SvLibConstantTerm {

  @Serial private static final long serialVersionUID = 6285135404459264229L;
  private final FloatingPointNumber value;
  private final FileLocation fileLocation;

  public SvLibFloatingPointConstantTerm(FloatingPointNumber pValue, FileLocation pFileLocation) {
    value = pValue;
    fileLocation = pFileLocation;
  }

  @Override
  public FloatingPointNumber getValue() {
    return value;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public @NonNull SvLibType getExpressionType() {
    return new SvLibSmtLibFloatingPointType(
        value.getExponentSize(), value.getMantissaSizeWithHiddenBit());
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  /** The given value as a bitvector literal of the given number of bits. */
  private static String toBitVectorLiteral(BigInteger pValue, int pBits) {
    StringBuilder bits = new StringBuilder("#b");
    for (int bit = pBits - 1; bit >= 0; bit--) {
      bits.append(pValue.testBit(bit) ? '1' : '0');
    }
    return bits.toString();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(fp "
        + toBitVectorLiteral(value.getMathSign().isNegative() ? BigInteger.ONE : BigInteger.ZERO, 1)
        + " "
        + toBitVectorLiteral(value.getExponent(), value.getExponentSize())
        + " "
        + toBitVectorLiteral(value.getMantissa(), value.getMantissaSizeWithoutHiddenBit())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
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

    return pO instanceof SvLibFloatingPointConstantTerm other && value.equals(other.value);
  }
}
