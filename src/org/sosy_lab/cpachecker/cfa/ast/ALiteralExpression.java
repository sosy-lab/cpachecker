// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class ALiteralExpression extends AbstractExpression {

  private static final long serialVersionUID = -457755136896976625L;

  protected ALiteralExpression(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
  }

  public abstract Object getValue();

  @Override
  public String toParenthesizedASTString(boolean pQualified) {
    // literal expression never need parentheses, are not qualified
    return toParenthesizedASTString();
  }

  @Override
  public String toParenthesizedASTString() {
    // literal expression never need parentheses, are not qualified
    return toASTString();
  }

  @Override
  public String toASTString(boolean pQualified) {
    // literal expression are never qualified
    return toASTString();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ALiteralExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
