// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class represents initializer expressions in variable and field declarations.
 *
 */
public final class JInitializerExpression extends AInitializerExpression implements JInitializer {

  private static final long serialVersionUID = 7168455809394583220L;

  public JInitializerExpression(FileLocation pFileLocation, JExpression pExpression) {
    super(pFileLocation, pExpression);
  }

  @Override
  public JExpression getExpression() {
    return (JExpression) super.getExpression();
  }

  @Override
  public <R, X extends Exception> R accept(JAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
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

    if (!(obj instanceof JInitializerExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
