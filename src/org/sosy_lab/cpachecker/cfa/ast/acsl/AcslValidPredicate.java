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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

public final class AcslValidPredicate extends AbstractExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = -69678901611906290L;
  private final AcslMemoryLocationSet memoryLocationSet;

  // Needs to be null for serialization
  private final @Nullable AcslLabel optionalLabel;

  public AcslValidPredicate(
      FileLocation pFileLocation, AcslMemoryLocationSet pMemoryLocationSet, AcslLabel pLabel) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    memoryLocationSet = checkNotNull(pMemoryLocationSet);
    optionalLabel = checkNotNull(pLabel);
  }

  public AcslValidPredicate(FileLocation pFileLocation, AcslMemoryLocationSet pMemoryLocationSet) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    memoryLocationSet = checkNotNull(pMemoryLocationSet);
    optionalLabel = null;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public AcslMemoryLocationSet getMemoryLocationSet() {
    return memoryLocationSet;
  }

  public Optional<AcslLabel> getLabel() {
    return Optional.ofNullable(optionalLabel);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "\\valid(" + memoryLocationSet.toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + super.hashCode();
    result = prime * result + memoryLocationSet.hashCode();
    result = prime * result + Objects.hashCode(optionalLabel);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslValidPredicate other
        && super.equals(other)
        && other.memoryLocationSet.equals(memoryLocationSet)
        && Objects.equals(other.optionalLabel, optionalLabel);
  }
}
