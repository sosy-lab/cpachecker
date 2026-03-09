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

public final class AcslPredicateDeclarationPredicate implements AcslPredicate {

  private final FileLocation fileLocation;
  private final AcslPredicateType type;
  private final String predicate;
  private final ImmutableList<AcslTerm> parameters;

  public AcslPredicateDeclarationPredicate(
      FileLocation pFileLocation,
      AcslPredicateType pType,
      String pIdPredicate,
      List<AcslTerm> pParameters) {
    type = pType;
    predicate = pIdPredicate;
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
    StringBuilder astString = new StringBuilder(predicate + "(");
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
    } else {
      return p0 instanceof AcslPredicateDeclarationPredicate other
          && predicate.equals(other.predicate)
          && parameters.equals(other.parameters);
    }
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(predicate);
    hash = hash * prime * Objects.hashCode(parameters);
    return hash;
  }
}
