// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslFunctionType;

public final class AcslFunctionDeclaration extends AFunctionDeclaration
    implements AcslLogicDeclaration {

  @Serial private static final long serialVersionUID = 6405702654281466421L;
  private final List<AcslTypeVariableDeclaration> polymorphicTypes;

  public AcslFunctionDeclaration(
      FileLocation pFileLocation,
      AcslFunctionType pType,
      String pName,
      String pOrigName,
      List<AcslTypeVariableDeclaration> pPolymorphicTypes,
      List<AcslParameterDeclaration> pParameters) {
    super(
        checkNotNull(pFileLocation),
        checkNotNull(pType),
        checkNotNull(pName),
        checkNotNull(pOrigName),
        checkNotNull(pParameters));
    polymorphicTypes = checkNotNull(pPolymorphicTypes);
    checkArgument(
        Lists.transform(pParameters, AcslParameterDeclaration::getType)
            .equals(pType.getParameters()),
        "The parameters %s do not have the parameter types of %s",
        pParameters,
        pType);
    checkArgument(
        ImmutableSet.copyOf(Lists.transform(pParameters, AcslParameterDeclaration::getName)).size()
            == pParameters.size(),
        "The parameters %s have different names",
        pParameters);
    checkArgument(
        ImmutableSet.copyOf(
                    Lists.transform(pPolymorphicTypes, AcslTypeVariableDeclaration::getName))
                .size()
            == pPolymorphicTypes.size(),
        "The type variables %s have different names",
        pPolymorphicTypes);
  }

  public List<AcslTypeVariableDeclaration> getPolymorphicTypes() {
    return polymorphicTypes;
  }

  @Override
  public AcslFunctionType getType() {
    return (AcslFunctionType) super.getType();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<AcslParameterDeclaration> getParameters() {
    return (ImmutableList<AcslParameterDeclaration>) super.getParameters();
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
    return "logic "
        + getType()
            .getReturnType()
            .toASTString(
                AcslLogicDeclaration.polyIdWithParameters(
                    getName(), polymorphicTypes, getParameters()));
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 8;
    result = prime * result + super.hashCode();
    result = prime * result + polymorphicTypes.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslFunctionDeclaration other
        && super.equals(other)
        && other.polymorphicTypes.equals(polymorphicTypes);
  }
}
