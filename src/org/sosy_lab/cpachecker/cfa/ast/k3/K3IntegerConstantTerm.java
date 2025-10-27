// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3IntegerConstantTerm implements K3ConstantTerm {

  @Serial private static final long serialVersionUID = 7803396078401840337L;
  private final BigInteger value;
  private final FileLocation fileLocation;

  public K3IntegerConstantTerm(BigInteger pValue, FileLocation pFileLocation) {
    value = pValue;
    fileLocation = pFileLocation;
  }

  @Override
  public <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public K3Type getExpressionType() {
    return K3SmtLibType.INT;
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

    return pO instanceof K3IntegerConstantTerm other && value.equals(other.value);
  }
}
