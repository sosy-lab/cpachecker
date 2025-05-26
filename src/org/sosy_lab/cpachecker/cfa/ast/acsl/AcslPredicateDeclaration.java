// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

// Formally a predicate is not a function, but it can be expressed as a function which returns
// booleans
public final class AcslPredicateDeclaration extends AFunctionDeclaration
    implements AcslLogicDeclaration {

  @Serial private static final long serialVersionUID = -814553465151276L;
  private final List<AcslTypeVariableDeclaration> polymorphicTypes;

  public AcslPredicateDeclaration(
      FileLocation pFileLocation,
      AcslPredicateType pType,
      String pName,
      String pOrigName,
      List<AcslTypeVariableDeclaration> pPolymorphicTypes,
      List<AcslParameterDeclaration> pParameters) {
    super(pFileLocation, pType, pName, pOrigName, pParameters);
    polymorphicTypes = pPolymorphicTypes;
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pName);
    checkNotNull(pOrigName);
    checkNotNull(pPolymorphicTypes);
    checkNotNull(pParameters);
  }

  public List<AcslTypeVariableDeclaration> getPolymorphicTypes() {
    return polymorphicTypes;
  }

  @Override
  public AcslPredicateType getType() {
    return (AcslPredicateType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(AcslSimpleDeclarationVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String typedDeclarationString() {
    return getType().toASTString(getName())
        + " "
        + getName()
        + "<"
        + FluentIterable.from(getPolymorphicTypes())
            .transform(t -> Objects.requireNonNull(t).toString())
        + ">("
        + FluentIterable.from(getParameters())
            .transform(e -> Objects.requireNonNull(e).toASTString())
            .join(Joiner.on(", "))
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 5;
    int result = 8;
    result = prime * result + super.hashCode();
    result = prime * result + Objects.hashCode(polymorphicTypes);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslPredicateDeclaration other
        && super.equals(other)
        && Objects.equals(other.polymorphicTypes, polymorphicTypes);
  }
}
