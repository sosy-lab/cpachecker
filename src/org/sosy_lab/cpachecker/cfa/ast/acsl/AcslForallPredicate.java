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
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslForallPredicate extends AcslQuantifiedPredicate {

  @Serial private static final long serialVersionUID = -814842855251151276L;

  public AcslForallPredicate(
      FileLocation pFileLocation,
      List<AcslParameterDeclaration> pBinders,
      AcslPredicate pPredicate) {
    super(pFileLocation, pBinders, pPredicate);
    checkNotNull(pFileLocation);
    checkNotNull(pBinders);
    checkNotNull(pPredicate);
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
    return toString("\\forall");
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toString("\\forall");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslForallPredicate other && super.equals(other);
  }
}
