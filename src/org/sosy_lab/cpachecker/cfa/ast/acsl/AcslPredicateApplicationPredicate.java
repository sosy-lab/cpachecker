// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

public final class AcslPredicateApplicationPredicate extends AbstractExpression
    implements AcslPredicate {

  @Serial private static final long serialVersionUID = 172251217614402479L;
  private final AcslPredicateDeclaration predicateDeclaration;
  private final ImmutableList<AcslTerm> parameters;

  public AcslPredicateApplicationPredicate(
      FileLocation pFileLocation,
      AcslPredicateDeclaration pPredicateDeclaration,
      List<AcslTerm> pParameters) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    checkArgument(
        AcslFunctionCallTerm.argumentsFit(
            pParameters, pPredicateDeclaration.getType().getParameters()),
        "%s cannot be applied to %s",
        pPredicateDeclaration,
        pParameters);
    predicateDeclaration = pPredicateDeclaration;
    parameters = ImmutableList.copyOf(pParameters);
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public AcslPredicateDeclaration getPredicateDeclaration() {
    return predicateDeclaration;
  }

  public List<AcslTerm> getParameters() {
    return parameters;
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    // Parentheses would make the application a term
    return toASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return predicateDeclaration.getName()
        + "("
        + Joiner.on(", ")
            .join(Lists.transform(parameters, term -> term.toASTString(pAAstNodeRepresentation)))
        + ")";
  }

  @Override
  public boolean equals(Object p0) {
    if (this == p0) {
      return true;
    }

    return p0 instanceof AcslPredicateApplicationPredicate other
        && super.equals(other)
        && predicateDeclaration.equals(other.predicateDeclaration)
        && parameters.equals(other.parameters);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash + super.hashCode();
    hash = prime * hash + predicateDeclaration.hashCode();
    hash = prime * hash + parameters.hashCode();
    return hash;
  }
}
