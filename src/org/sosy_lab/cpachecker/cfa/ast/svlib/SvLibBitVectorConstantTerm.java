// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import java.io.Serial;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibBitVectorConstantTerm implements SvLibConstantTerm {

  /** The ways to write a bitvector literal in SMT-LIB. */
  public enum Notation {
    BINARY,
    HEXADECIMAL,
    /** {@code (_ bvN size)} */
    INDEXED
  }

  @Serial private static final long serialVersionUID = 7746029699505352690L;
  private final BigInteger value;
  private final int size;
  // Kept, so that an annotation is written in a witness as the program writes it
  private final Notation notation;
  private final FileLocation fileLocation;

  public SvLibBitVectorConstantTerm(BigInteger pValue, int pSize, FileLocation pFileLocation) {
    this(pValue, pSize, Notation.INDEXED, pFileLocation);
  }

  public SvLibBitVectorConstantTerm(
      BigInteger pValue, int pSize, Notation pNotation, FileLocation pFileLocation) {
    checkArgument(
        pNotation != Notation.HEXADECIMAL || pSize % 4 == 0,
        "A hexadecimal literal has a multiple of 4 bits, not %s",
        pSize);
    value = pValue;
    size = pSize;
    notation = pNotation;
    fileLocation = pFileLocation;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  public int getSize() {
    return size;
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public @NonNull SvLibType getExpressionType() {
    return new SvLibSmtLibBitVectorType(size);
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

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return switch (notation) {
      case BINARY -> "#b" + Strings.padStart(value.toString(2), size, '0');
      case HEXADECIMAL -> "#x" + Strings.padStart(value.toString(16), size / 4, '0');
      case INDEXED -> "(_ bv" + value + " " + size + ")";
    };
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    return value.hashCode() * 31 + size;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibBitVectorConstantTerm other
        && size == other.size
        && value.equals(other.value);
  }
}
