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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslValidExpression implements AcslExpression {
  @Serial private static final long serialVersionUID = -69678901611906290L;
  private final FileLocation fileLocation;
  private final AcslType type;
  private final AcslMemoryLocationSet memoryLocationSet;

  private final Optional<AcslLabel> optionalLabel;

  public AcslValidExpression(
      FileLocation pFileLocation,
      AcslType pType,
      AcslMemoryLocationSet pMemoryLocationSet,
      AcslLabel pLabel) {
    fileLocation = pFileLocation;
    type = pType;
    memoryLocationSet = pMemoryLocationSet;
    optionalLabel = Optional.of(pLabel);
  }

  public AcslValidExpression(
      FileLocation pFileLocation, AcslType pType, AcslMemoryLocationSet pMemoryLocationSet) {
    fileLocation = pFileLocation;
    type = pType;
    memoryLocationSet = pMemoryLocationSet;
    optionalLabel = Optional.empty();
  }

  @Override
  public <R, X extends Exception> R accept(AcslExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public Type getExpressionType() {
    return type;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\valid(" + memoryLocationSet.toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\valid(" + memoryLocationSet.toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + Objects.hashCode(fileLocation);
    result = prime * result + Objects.hashCode(memoryLocationSet);
    result = prime * result + Objects.hashCode(optionalLabel);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslValidExpression other
        && Objects.equals(other.type, type)
        && Objects.equals(other.fileLocation, fileLocation)
        && Objects.equals(other.memoryLocationSet, memoryLocationSet)
        && Objects.equals(other.optionalLabel, optionalLabel);
  }
}
