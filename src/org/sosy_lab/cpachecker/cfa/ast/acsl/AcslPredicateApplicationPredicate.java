// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

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
    Verify.verify(
        pParameters.size() == pPredicateDeclaration.getParameters().size(),
        "Expected %s parameters but got %s.",
        pPredicateDeclaration.getParameters().size(),
        pParameters.size());
    for (int i = 0; i < pParameters.size(); i++) {
      AcslType providedType = pParameters.get(i).getExpressionType();
      AcslType expectedType = pPredicateDeclaration.getType().getParameters().get(i);
      Verify.verify(
          providedTypeMatchesExpectedType(providedType, expectedType),
          "Provided parameter %s is not of the expected type.",
          i);
    }
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
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return predicateDeclaration.getName() + "(" + Joiner.on(", ").join(parameters) + ")";
  }

  private boolean providedTypeMatchesExpectedType(AcslType provided, AcslType expected) {
    AcslType generalType = AcslType.mostGeneralType(provided, expected);
    return expected.equals(generalType) || provided.equals(generalType);
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
