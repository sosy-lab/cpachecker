// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class K3NumeralConstantTerm implements K3ConstantTerm {

  private final BigInteger value;
  private final FileLocation fileLocation;

  public K3NumeralConstantTerm(BigInteger pValue, FileLocation pFileLocation) {
    value = pValue;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public Type getExpressionType() {
    return K3ConstantType.NUMERAL_CONSTANT;
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return value.toString();
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return value.toString();
  }

  @Override
  public BigInteger getValue() {
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

    return pO instanceof K3NumeralConstantTerm other && value.equals(other.value);
  }
}
