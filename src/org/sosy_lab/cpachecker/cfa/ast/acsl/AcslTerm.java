// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AcslTerm implements AcslAstNode
    permits AcslBinaryTerm,
        AcslIdTerm,
        AcslLiteralTerm,
        AcslOldTerm,
        AcslResultTerm,
        AcslUnaryTerm {

  @Serial private static final long serialVersionUID = 8144237675011353L;
  private final AcslType type;
  private final FileLocation location;

  protected AcslTerm(FileLocation pLocation, AcslType pType) {
    type = pType;
    location = pLocation;
  }

  public AcslType getExpressionType() {
    return type;
  }

  @Override
  public FileLocation getFileLocation() {
    return location;
  }

  public abstract <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X;

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(location);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslTerm other
        && Objects.equals(other.type, type)
        && Objects.equals(other.location, location);
  }
}
