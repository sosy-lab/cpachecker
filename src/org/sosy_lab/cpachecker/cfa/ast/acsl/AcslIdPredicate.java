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
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslIdPredicate extends AIdExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = -814550123151276L;

  public AcslIdPredicate(FileLocation pFileLocation, AcslPredicateDeclaration pDeclaration) {
    super(pFileLocation, pDeclaration);
    checkNotNull(pFileLocation);
    checkNotNull(pDeclaration);
  }

  @Override
  public AcslPredicateDeclaration getDeclaration() {
    return (AcslPredicateDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
