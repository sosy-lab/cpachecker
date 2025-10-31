// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3IdTerm extends AIdExpression implements K3Term {
  @Serial private static final long serialVersionUID = 5782817996036730363L;

  public K3IdTerm(K3SimpleDeclaration pVariable, FileLocation pFileLocation) {
    super(pFileLocation, pVariable);
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public K3SimpleDeclaration getDeclaration() {
    return (K3SimpleDeclaration) super.getDeclaration();
  }

  @Override
  public <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public K3Type getExpressionType() {
    return (K3Type) super.getExpressionType();
  }
}
