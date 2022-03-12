// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * Abstract class for side-effect free expressions. This class is only SuperClass of all abstract
 * Classes and their Subclasses. The Interface {@link AExpression} contains all language specific
 * AST Nodes as well.
 */
public abstract class AbstractExpression extends AbstractRightHandSide implements AExpression {

  private static final long serialVersionUID = 7019956361956900L;

  protected AbstractExpression(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
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

    if (!(obj instanceof AbstractExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
