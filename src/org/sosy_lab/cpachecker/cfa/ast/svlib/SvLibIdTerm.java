// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibIdTerm extends AIdExpression implements SvLibTerm, SvLibLeftHandSide {
  @Serial private static final long serialVersionUID = 5782817996036730363L;

  public SvLibIdTerm(SvLibSimpleDeclaration pVariable, FileLocation pFileLocation) {
    super(pFileLocation, pVariable);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  @NonNull
  public SvLibSimpleDeclaration getDeclaration() {
    return (SvLibSimpleDeclaration) super.getDeclaration();
  }

  @Override
  public SvLibType getExpressionType() {
    return (SvLibType) super.getExpressionType();
  }
}
