// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import java.io.Serial;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3IncorrectTagProperty extends K3ViolatedProperty {
  @Serial private static final long serialVersionUID = 5489687141447266694L;
  private final Optional<K3TagReference> k3TagReference;
  private final Set<K3TagProperty> violatedProperties;

  public K3IncorrectTagProperty(
      FileLocation pFileLocation,
      K3TagReference pK3TagReference,
      Set<K3TagProperty> pViolatedTerm) {
    super(pFileLocation);
    k3TagReference = Optional.of(pK3TagReference);
    violatedProperties = pViolatedTerm;
  }

  public K3IncorrectTagProperty(FileLocation pFileLocation, Set<K3TagProperty> pViolatedTerm) {
    super(pFileLocation);
    k3TagReference = Optional.empty();
    violatedProperties = pViolatedTerm;
  }

  public Optional<K3TagReference> getK3TagReference() {
    return k3TagReference;
  }

  public Set<K3TagProperty> getViolatedProperties() {
    return violatedProperties;
  }

  @Override
  <R, X extends Exception> R accept(K3TraceElementVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(incorrect-tag "
        + (k3TagReference.isPresent()
            ? k3TagReference.orElseThrow().toASTString(pAAstNodeRepresentation)
            : "")
        + " "
        + Joiner.on(" ").join(violatedProperties.stream().map(K3AstNode::toASTString).toList())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + k3TagReference.hashCode();
    result = prime * result + violatedProperties.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3IncorrectTagProperty other
        && k3TagReference.equals(other.k3TagReference)
        && violatedProperties.equals(other.violatedProperties);
  }
}
