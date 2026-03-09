// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslPredicateApplicationPredicate implements AcslPredicate {

  private final FileLocation fileLocation;
  private final AcslPredicateDeclaration predicateDeclaration;
  private final AcslPredicateType predicateType;
  private final ImmutableList<AcslTerm> parameters;

  public AcslPredicateApplicationPredicate(
      FileLocation pFileLocation,
      AcslPredicateDeclaration pPredicateDeclaration,
      AcslPredicateType pPredicateType,
      List<AcslTerm> pParameters) {
    predicateDeclaration = pPredicateDeclaration;
    predicateType = pPredicateType;
    parameters = ImmutableList.copyOf(pParameters);
    fileLocation = pFileLocation;
  }

  @Serial private static final long serialVersionUID = 448373275534775671L;

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public Type getExpressionType() {
    return AcslBuiltinLogicType.BOOLEAN;
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
    StringBuilder astString = new StringBuilder(predicateDeclaration + "(");
    String paramString = Joiner.on(", ").join(parameters.stream().toList());
    astString.append(paramString);
    astString.append(")");
    return astString.toString();
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(" + toASTString(pAAstNodeRepresentation) + ")";
  }

  @Override
  public boolean equals(Object p0) {
    if (this == p0) {
      return true;
    }
    return p0 instanceof AcslPredicateApplicationPredicate other
        && predicateDeclaration.equals(other.predicateDeclaration)
        && predicateType.equals(other.predicateType)
        && parameters.equals(other.parameters);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(predicateDeclaration);
    hash = prime * hash * Objects.hashCode(predicateType);
    hash = hash * prime * Objects.hashCode(parameters);
    return hash;
  }
}
