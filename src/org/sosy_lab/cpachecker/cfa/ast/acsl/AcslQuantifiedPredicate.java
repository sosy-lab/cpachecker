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
import com.google.common.collect.Lists;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

public abstract sealed class AcslQuantifiedPredicate extends AbstractExpression
    implements AcslPredicate permits AcslExistsPredicate, AcslForallPredicate {

  @Serial private static final long serialVersionUID = -81123952385151276L;

  private final List<AcslParameterDeclaration> binders;
  private final AcslPredicate predicate;

  AcslQuantifiedPredicate(
      FileLocation pFileLocation,
      List<AcslParameterDeclaration> pBinders,
      AcslPredicate pPredicate) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    binders = checkNotNull(pBinders);
    predicate = checkNotNull(pPredicate);
  }

  public List<AcslParameterDeclaration> getBinders() {
    return binders;
  }

  public AcslPredicate getPredicate() {
    return predicate;
  }

  abstract String getQuantifier();

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return getQuantifier()
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
        && other.getClass().equals(getClass())
        && super.equals(other)
        && other.binders.equals(binders)
        && other.predicate.equals(predicate);
  }
}
