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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

public final class AcslQuantifiedPredicate extends AbstractExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = -81123952385151276L;

  private final AcslQuantifier quantifier;
  private final ImmutableList<AcslParameterDeclaration> binders;
  private final AcslPredicate predicate;

  public AcslQuantifiedPredicate(
      FileLocation pFileLocation,
      AcslQuantifier pQuantifier,
      List<AcslParameterDeclaration> pBinders,
      AcslPredicate pPredicate) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    quantifier = checkNotNull(pQuantifier);
    binders = ImmutableList.copyOf(pBinders);
    predicate = checkNotNull(pPredicate);
  }

  public AcslQuantifier getQuantifier() {
    return quantifier;
  }

  public ImmutableList<AcslParameterDeclaration> getBinders() {
    return binders;
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return quantifier
        + " "
        + Joiner.on(", ")
            .join(Lists.transform(binders, binder -> binder.toASTString(pAAstNodeRepresentation)))
        + "; "
        + predicate.toASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 3;
    int result = 7;
    result = prime * result + super.hashCode();
    result = prime * result + quantifier.hashCode();
    result = prime * result + binders.hashCode();
    result = prime * result + predicate.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslQuantifiedPredicate other
        && super.equals(other)
        && other.quantifier == quantifier
        && other.binders.equals(binders)
        && other.predicate.equals(predicate);
  }

  public enum AcslQuantifier {
    FORALL("\\forall"),
    EXISTS("\\exists"),
    ;

    private final String keyword;

    AcslQuantifier(String pKeyword) {
      keyword = pKeyword;
    }

    public static AcslQuantifier of(String pKeyword) {
      for (AcslQuantifier quantifier : values()) {
        if (quantifier.keyword.equals(pKeyword)) {
          return quantifier;
        }
      }
      throw new IllegalArgumentException("Unknown quantifier: " + pKeyword);
    }

    @Override
    public String toString() {
      return keyword;
    }
  }
}
