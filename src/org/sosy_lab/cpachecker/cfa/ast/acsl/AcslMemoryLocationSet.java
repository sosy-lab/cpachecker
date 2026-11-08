// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslSetType;

public abstract sealed class AcslMemoryLocationSet extends AbstractAstNode implements AcslAstNode
    permits AcslMemoryLocationSetEmpty, AcslMemoryLocationSetTerm {

  @Serial private static final long serialVersionUID = 1456718713245456789L;

  private final AcslSetType type;

  protected AcslMemoryLocationSet(FileLocation pFileLocation, AcslSetType pType) {
    super(pFileLocation);
    type = checkNotNull(pType);
  }

  public AcslSetType getType() {
    return type;
  }

  public abstract <R, X extends Exception> R accept(AcslMemoryLocationSetVisitor<R, X> v) throws X;

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + type.hashCode();
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslMemoryLocationSet other
        && super.equals(other)
        && other.type.equals(type);
  }
}
