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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AcslMemoryLocationSet implements AcslAstNode
    permits AcslMemoryLocationSetEmpty, AcslMemoryLocationSetTerm {

  @Serial private static final long serialVersionUID = 1456718713245456789L;

  private final FileLocation fileLocation;
  private final AcslSetType type;

  protected AcslMemoryLocationSet(FileLocation pFileLocation, AcslSetType pType) {
    fileLocation = pFileLocation;
    type = pType;
    checkNotNull(pFileLocation);
    checkNotNull(pType);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  public AcslSetType getType() {
    return type;
  }

  public abstract <R, X extends Exception> R accept(AcslMemoryLocationSetVisitor<R, X> v) throws X;

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(fileLocation);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslMemoryLocationSet other
        && Objects.equals(other.type, type)
        && Objects.equals(other.fileLocation, fileLocation);
  }
}
